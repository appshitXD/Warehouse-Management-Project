package com.group4.warehouse.issue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.IsoTime;

/**
 * SQL for goods issues. Read-only: master (GOODS_ISSUE) and detail (GOODS_ISSUE_LINE).
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
