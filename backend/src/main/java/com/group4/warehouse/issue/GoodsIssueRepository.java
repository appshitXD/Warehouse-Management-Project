package com.group4.warehouse.issue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.DocumentIds;
import com.group4.warehouse.common.IsoTime;
import com.group4.warehouse.common.References;
import com.group4.warehouse.common.StockLedger;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Transactions;

/**
 * SQL for goods issues: master (GOODS_ISSUE) and detail (GOODS_ISSUE_LINE).
 *
 * An issue takes stock OUT of bins, but only once the goods leave: while the status is
 * Pending nothing changes in PRODUCT_BIN, so a Pending issue can be edited or deleted freely.
 * Moving it to Shipped or Completed subtracts every line from PRODUCT_BIN in the same
 * transaction; from then on the issue is locked (only Shipped -> Completed is allowed)
 * and it can no longer be deleted.
 *
 * Every value from the request goes through a "?" placeholder (PreparedStatement),
 * so input like  GI01' OR '1'='1  is compared as plain text and matches nothing.
 */
@Repository
public class GoodsIssueRepository {

    // GOODS_ISSUE -> CUSTOMER_ORDER -> CUSTOMER: an issue has no Customer_ID of its own,
    // the customer is reached through the order it ships.
    private static final String SELECT_ISSUES = """
            SELECT gi.Issue_ID, gi.Issue_Date, gi.Status, gi.State_Of_Good, gi.Order_ID,
                   c.Customer_ID, c.Name AS Customer_Name
            FROM GOODS_ISSUE gi
            JOIN CUSTOMER_ORDER co ON co.Order_ID   = gi.Order_ID
            JOIN CUSTOMER c        ON c.Customer_ID = co.Customer_ID
            """;

    private final DataSource dataSource;

    public GoodsIssueRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** All issues, newest first. q (optional) searches issue ID, order ID and customer name. */
    public List<GoodsIssue> findAll(String q) {
        String sql = SELECT_ISSUES + """
                WHERE (? IS NULL OR gi.Issue_ID LIKE ? OR gi.Order_ID LIKE ? OR c.Name LIKE ?)
                ORDER BY gi.Issue_Date DESC, gi.Issue_ID DESC
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<GoodsIssue> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapIssue(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "goods issue");
        }
    }

    public Optional<GoodsIssue> findById(String id) {
        String sql = SELECT_ISSUES + " WHERE gi.Issue_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapIssue(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "goods issue");
        }
    }

    /** The lines of one issue with the product name, in line order. Empty list if none. */
    public List<GoodsIssueLine> findLines(String issueId) {
        String sql = """
                SELECT gil.Issue_Line_ID, gil.Issue_ID, gil.Product_ID, p.Product_Name, p.Unit,
                       gil.Warehouse_ID, gil.Zone_ID, gil.Aisle_ID, gil.Position, gil.Quantity
                FROM GOODS_ISSUE_LINE gil
                JOIN PRODUCT p ON p.Product_ID = gil.Product_ID
                WHERE gil.Issue_ID = ?
                ORDER BY gil.Issue_Line_ID
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                List<GoodsIssueLine> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new GoodsIssueLine(
                            rs.getInt("Issue_Line_ID"),
                            rs.getString("Issue_ID"),
                            rs.getString("Product_ID"),
                            rs.getString("Product_Name"),
                            rs.getString("Unit"),
                            BinId.from(rs, "").toString(),
                            rs.getInt("Quantity")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "goods issue line");
        }
    }

    /**
     * Creates the issue. status null = the column DEFAULT ('Pending').
     * If it is created as Shipped or Completed, its lines leave stock straight away.
     * @return the issue ID (generated if id is null)
     */
    public String create(String id, LocalDate date, String orderId, String stateOfGood, String status,
                         List<StockLine> lines) {
        return Transactions.run(dataSource, "goods issue", conn -> {
            References.require(conn, "CUSTOMER_ORDER", "Order_ID", orderId, "Customer order");
            for (StockLine line : lines) {
                StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
            }
            String issueId = id != null ? id : DocumentIds.next(conn, "GOODS_ISSUE", "Issue_ID", "GI");

            // Leaving Status out of the INSERT lets DEFAULT 'Pending' fill it in.
            String sql = status == null
                    ? "INSERT INTO GOODS_ISSUE (Issue_ID, Issue_Date, State_Of_Good, Order_ID) VALUES (?, ?, ?, ?)"
                    : "INSERT INTO GOODS_ISSUE (Issue_ID, Issue_Date, State_Of_Good, Order_ID, Status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, issueId);
                ps.setObject(2, date);
                ps.setString(3, stateOfGood);
                ps.setString(4, orderId);
                if (status != null) {
                    ps.setString(5, status);
                }
                ps.executeUpdate();
            }
            insertLines(conn, issueId, lines);
            if (status != null && IssueStatus.shipped(status)) {
                StockLedger.takeOut(conn, lines);
            }
            return issueId;
        });
    }

    /**
     * Every argument null = keep. See the class comment for what a status allows.
     * @return false if no issue has this ID
     */
    public boolean update(String id, LocalDate date, String orderId, String stateOfGood, String status,
                          List<StockLine> lines) {
        return Transactions.run(dataSource, "goods issue", conn -> {
            String current = lockStatus(conn, id);
            if (current == null) {
                return false;
            }
            String next = status == null ? current : status;
            if (IssueStatus.rank(next) < IssueStatus.rank(current)) {
                throw ApiException.locked("Goods issue " + id + " is " + current
                        + ", so it cannot go back to " + next + ".");
            }
            if (IssueStatus.shipped(current)
                    && (date != null || orderId != null || stateOfGood != null || lines != null)) {
                throw ApiException.locked("Goods issue " + id + " is " + current
                        + ", so its goods have already left the warehouse. Only its status can still move on"
                        + " (Shipped -> Completed).");
            }

            if (orderId != null) {
                References.require(conn, "CUSTOMER_ORDER", "Order_ID", orderId, "Customer order");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE GOODS_ISSUE
                    SET Issue_Date    = COALESCE(?, Issue_Date),
                        Order_ID      = COALESCE(?, Order_ID),
                        State_Of_Good = COALESCE(?, State_Of_Good),
                        Status        = ?
                    WHERE Issue_ID = ?
                    """)) {
                ps.setObject(1, date);
                ps.setString(2, orderId);
                ps.setString(3, stateOfGood);
                ps.setString(4, next);
                ps.setString(5, id);
                ps.executeUpdate();
            }
            if (lines != null) {
                for (StockLine line : lines) {
                    StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
                }
                deleteLines(conn, id);
                insertLines(conn, id, lines);
            }
            // Pending -> Shipped/Completed: the goods leave now.
            if (!IssueStatus.shipped(current) && IssueStatus.shipped(next)) {
                StockLedger.takeOut(conn, stockLines(conn, id));
            }
            return true;
        });
    }

    /** Only a Pending issue can be deleted (its goods have not left yet). @return false if not found */
    public boolean delete(String id) {
        return Transactions.run(dataSource, "goods issue", conn -> {
            String current = lockStatus(conn, id);
            if (current == null) {
                return false;
            }
            if (IssueStatus.shipped(current)) {
                throw ApiException.locked("Goods issue " + id + " is " + current
                        + ", so it cannot be deleted. Only Pending issues can be deleted.");
            }
            deleteLines(conn, id);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GOODS_ISSUE WHERE Issue_ID = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            return true;
        });
    }

    /** Current status, row locked until COMMIT; null if the issue does not exist. */
    private static String lockStatus(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Status FROM GOODS_ISSUE WHERE Issue_ID = ? FOR UPDATE")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void insertLines(Connection conn, String issueId, List<StockLine> lines) throws SQLException {
        String sql = """
                INSERT INTO GOODS_ISSUE_LINE
                    (Issue_ID, Issue_Line_ID, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int lineNo = 1;
            for (StockLine line : lines) {
                ps.setString(1, issueId);
                ps.setInt(2, lineNo++);
                ps.setString(3, line.productId());
                StockLedger.setBin(ps, 4, line.bin());
                ps.setInt(8, line.quantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void deleteLines(Connection conn, String issueId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GOODS_ISSUE_LINE WHERE Issue_ID = ?")) {
            ps.setString(1, issueId);
            ps.executeUpdate();
        }
    }

    private static List<StockLine> stockLines(Connection conn, String issueId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity
                FROM GOODS_ISSUE_LINE WHERE Issue_ID = ? ORDER BY Issue_Line_ID
                """)) {
            ps.setString(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockLine> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new StockLine(rs.getString("Product_ID"), BinId.from(rs, ""), rs.getInt("Quantity")));
                }
                return result;
            }
        }
    }

    private static GoodsIssue mapIssue(ResultSet rs) throws SQLException {
        return new GoodsIssue(
                rs.getString("Issue_ID"),
                IsoTime.date(rs, "Issue_Date"),
                rs.getString("Status"),
                rs.getString("State_Of_Good"),
                rs.getString("Order_ID"),
                rs.getString("Customer_ID"),
                rs.getString("Customer_Name"));
    }
}
