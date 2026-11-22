package com.group4.warehouse.count;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.DocumentIds;
import com.group4.warehouse.common.IsoTime;
import com.group4.warehouse.common.StockLedger;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Transactions;
import com.group4.warehouse.count.StockCount.CountLine;

/**
 * SQL for stock counts: STOCK_COUNT (header) and COUNT_LINE (lines).
 *
 * Each line stores two numbers: Recorded_Quantity, read from PRODUCT_BIN when the line is
 * saved, and Quantity_Counted, typed by staff. While the count is Pending or In Progress
 * nothing changes in PRODUCT_BIN. Setting it to Completed (one transaction):
 *   1. reads PRODUCT_BIN again, so Recorded_Quantity is the stock just before the adjustment
 *   2. sets PRODUCT_BIN.Quantity to the counted quantity
 * After that the count is locked; only a Pending count can be deleted.
 */
@Repository
public class StockCountRepository {

    private static final String ENTITY = "stock count";

    private static final String SELECT_LINES = """
            SELECT cl.Count_ID, cl.Line_No, cl.Product_ID, p.Product_Name, p.Unit,
                   cl.Warehouse_ID, cl.Zone_ID, cl.Aisle_ID, cl.Position,
                   cl.Recorded_Quantity, cl.Quantity_Counted
            FROM COUNT_LINE cl
            JOIN PRODUCT p ON p.Product_ID = cl.Product_ID
            """;

    private final DataSource dataSource;

    public StockCountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q matches count ID or status. Newest first, each with its lines. */
    public List<StockCount> findAll(String q) {
        String sql = """
                SELECT Count_ID, Count_Date, Count_Status FROM STOCK_COUNT
                WHERE ? IS NULL OR Count_ID LIKE ? OR Count_Status LIKE ?
                ORDER BY Count_Date DESC, Count_ID DESC
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection()) {
            Map<String, List<CountLine>> lines = allLines(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, q);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    List<StockCount> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapHeader(rs, lines.getOrDefault(rs.getString("Count_ID"), List.of())));
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, ENTITY);
        }
    }

    public Optional<StockCount> findById(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT Count_ID, Count_Date, Count_Status FROM STOCK_COUNT WHERE Count_ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapHeader(rs, lines(conn, id))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, ENTITY);
        }
    }

    /** status null = the column DEFAULT ('Pending'). @return the count ID (generated if id is null) */
    public String create(String id, LocalDateTime date, String status, List<StockLine> lines) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            for (StockLine line : lines) {
                StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
            }
            String countId = id != null ? id : DocumentIds.next(conn, "STOCK_COUNT", "Count_ID", "SC");

            // Leaving Count_Status out of the INSERT lets DEFAULT 'Pending' fill it in.
            String sql = status == null
                    ? "INSERT INTO STOCK_COUNT (Count_ID, Count_Date) VALUES (?, ?)"
                    : "INSERT INTO STOCK_COUNT (Count_ID, Count_Date, Count_Status) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, countId);
                ps.setObject(2, date);
                if (status != null) {
                    ps.setString(3, status);
                }
                ps.executeUpdate();
            }
            insertLines(conn, countId, lines);
            if (CountStatus.COMPLETED.equals(status)) {
                applyToStock(conn, countId);
            }
            return countId;
        });
    }

    /** Every argument null = keep. @return false if no count has this ID */
    public boolean update(String id, LocalDateTime date, String status, List<StockLine> lines) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            String current = lockStatus(conn, id);
            if (current == null) {
                return false;
            }
            if (CountStatus.COMPLETED.equals(current)) {
                throw ApiException.locked("Stock count " + id + " is Completed and has already been applied"
                        + " to stock, so it can no longer be changed.");
            }
            String next = status == null ? current : status;
            if (CountStatus.rank(next) < CountStatus.rank(current)) {
                throw ApiException.locked("Stock count " + id + " is " + current
                        + ", so it cannot go back to " + next + ".");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE STOCK_COUNT
                    SET Count_Date = COALESCE(?, Count_Date), Count_Status = ?
                    WHERE Count_ID = ?
                    """)) {
                ps.setObject(1, date);
                ps.setString(2, next);
                ps.setString(3, id);
                ps.executeUpdate();
            }
            if (lines != null) {
                for (StockLine line : lines) {
                    StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
                }
                deleteLines(conn, id);
                insertLines(conn, id, lines);
            }
            if (CountStatus.COMPLETED.equals(next)) {
                applyToStock(conn, id);
            }
            return true;
        });
    }

    /** Only a Pending count can be deleted. @return false if not found */
    public boolean delete(String id) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            String current = lockStatus(conn, id);
            if (current == null) {
                return false;
            }
            if (!CountStatus.PENDING.equals(current)) {
                throw ApiException.locked("Stock count " + id + " is " + current
                        + ", so it cannot be deleted. Only Pending counts can be deleted.");
            }
            deleteLines(conn, id);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM STOCK_COUNT WHERE Count_ID = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            return true;
        });
    }

    // ---- helpers -------------------------------------------------------------------------

    /**
     * Completing a count: for every line, store the stock just before the adjustment as
     * Recorded_Quantity, then make the counted quantity the new stock.
     */
    private static void applyToStock(Connection conn, String countId) throws SQLException {
        String sql = "UPDATE COUNT_LINE SET Recorded_Quantity = ? WHERE Count_ID = ? AND Line_No = ?";
        for (CountLine line : lines(conn, countId)) {
            BinId bin = BinId.parse(line.binId());
            int recorded = StockLedger.quantity(conn, line.productId(), bin);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, recorded);
                ps.setString(2, countId);
                ps.setInt(3, line.lineNo());
                ps.executeUpdate();
            }
            StockLedger.setTo(conn, line.productId(), bin, line.countedQuantity());
        }
    }

    /** Recorded_Quantity = the stock in PRODUCT_BIN now (0 if the product is not in that bin). */
    private static void insertLines(Connection conn, String countId, List<StockLine> lines) throws SQLException {
        String sql = """
                INSERT INTO COUNT_LINE
                    (Count_ID, Line_No, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position,
                     Recorded_Quantity, Quantity_Counted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int lineNo = 1;
            for (StockLine line : lines) {
                ps.setString(1, countId);
                ps.setInt(2, lineNo++);
                ps.setString(3, line.productId());
                StockLedger.setBin(ps, 4, line.bin());
                ps.setInt(8, StockLedger.quantity(conn, line.productId(), line.bin()));
                ps.setInt(9, line.quantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void deleteLines(Connection conn, String countId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM COUNT_LINE WHERE Count_ID = ?")) {
            ps.setString(1, countId);
            ps.executeUpdate();
        }
    }

    /** Current status, row locked until COMMIT; null if the count does not exist. */
    private static String lockStatus(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Count_Status FROM STOCK_COUNT WHERE Count_ID = ? FOR UPDATE")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static List<CountLine> lines(Connection conn, String countId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES + " WHERE cl.Count_ID = ? ORDER BY cl.Line_No")) {
            ps.setString(1, countId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CountLine> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapLine(rs));
                }
                return result;
            }
        }
    }

    private static Map<String, List<CountLine>> allLines(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES + " ORDER BY cl.Count_ID, cl.Line_No");
             ResultSet rs = ps.executeQuery()) {
            Map<String, List<CountLine>> result = new LinkedHashMap<>();
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("Count_ID"), k -> new ArrayList<>()).add(mapLine(rs));
            }
            return result;
        }
    }

    private static StockCount mapHeader(ResultSet rs, List<CountLine> lines) throws SQLException {
        return new StockCount(
                rs.getString("Count_ID"),
                IsoTime.dateTime(rs, "Count_Date"),
                rs.getString("Count_Status"),
                lines);
    }

    private static CountLine mapLine(ResultSet rs) throws SQLException {
        int recorded = rs.getInt("Recorded_Quantity");
        int counted = rs.getInt("Quantity_Counted");
        return new CountLine(
                rs.getInt("Line_No"),
                rs.getString("Product_ID"),
                rs.getString("Product_Name"),
                rs.getString("Unit"),
                BinId.from(rs, "").toString(),
                recorded,
                counted,
                counted - recorded);
    }
}
