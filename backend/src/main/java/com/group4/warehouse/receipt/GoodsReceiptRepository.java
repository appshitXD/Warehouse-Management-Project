package com.group4.warehouse.receipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
import com.group4.warehouse.common.References;
import com.group4.warehouse.common.StockLedger;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Transactions;
import com.group4.warehouse.receipt.GoodsReceipt.ReceiptLine;

/**
 * SQL for goods receipts. A receipt puts stock INTO bins, so every save is one transaction
 * over three tables: GOODS_RECEIPT (header), GOODS_RECEIPT_LINE (lines) and PRODUCT_BIN (stock).
 *
 *   create  INSERT header + lines, add each line to PRODUCT_BIN
 *   update  UPDATE header; if lines are sent: replace them and move the stock by the difference
 *   delete  take the received stock back out of PRODUCT_BIN, DELETE lines + header
 *
 * GOODS_RECEIPT has no status column, so a receipt counts as received as soon as it is saved.
 */
@Repository
public class GoodsReceiptRepository {

    private static final String ENTITY = "goods receipt";

    private static final String SELECT_HEADERS = """
            SELECT gr.Receipt_ID, gr.Receipt_Date, gr.Supplier_ID, s.Supplier_Name
            FROM GOODS_RECEIPT gr
            JOIN SUPPLIER s ON s.Supplier_ID = gr.Supplier_ID
            """;

    private static final String SELECT_LINES = """
            SELECT grl.Receipt_ID, grl.Line_Num, grl.Product_ID, p.Product_Name, p.Unit,
                   grl.Warehouse_ID, grl.Zone_ID, grl.Aisle_ID, grl.Position, grl.Quantity
            FROM GOODS_RECEIPT_LINE grl
            JOIN PRODUCT p ON p.Product_ID = grl.Product_ID
            """;

    private final DataSource dataSource;

    public GoodsReceiptRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q matches receipt ID or supplier name. Newest first, each with its lines. */
    public List<GoodsReceipt> findAll(String q) {
        String sql = SELECT_HEADERS + """
                WHERE ? IS NULL OR gr.Receipt_ID LIKE ? OR s.Supplier_Name LIKE ?
                ORDER BY gr.Receipt_Date DESC, gr.Receipt_ID DESC
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection()) {
            Map<String, List<ReceiptLine>> lines = allLines(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, q);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    List<GoodsReceipt> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapHeader(rs, lines.getOrDefault(rs.getString("Receipt_ID"), List.of())));
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, ENTITY);
        }
    }

    public Optional<GoodsReceipt> findById(String id) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, ENTITY);
        }
    }

    /** Creates the receipt and adds its stock. @return the receipt ID (generated if id is null). */
    public String create(String id, LocalDate date, String supplierId, List<StockLine> lines) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            References.require(conn, "SUPPLIER", "Supplier_ID", supplierId, "Supplier");
            for (StockLine line : lines) {
                StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
            }
            String receiptId = id != null ? id : DocumentIds.next(conn, "GOODS_RECEIPT", "Receipt_ID", "GR");

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO GOODS_RECEIPT (Receipt_ID, Receipt_Date, Supplier_ID) VALUES (?, ?, ?)")) {
                ps.setString(1, receiptId);
                ps.setObject(2, date);
                ps.setString(3, supplierId);
                ps.executeUpdate();
            }
            insertLines(conn, receiptId, lines);
            StockLedger.apply(conn, StockLine.totals(lines));
            return receiptId;
        });
    }

    /**
     * date / supplierId null = keep. lines null = keep the lines; otherwise replace them and
     * change PRODUCT_BIN by (new lines - old lines).
     * @return false if no receipt has this ID
     */
    public boolean update(String id, LocalDate date, String supplierId, List<StockLine> lines) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            if (!lockHeader(conn, id)) {
                return false;
            }
            if (supplierId != null) {
                References.require(conn, "SUPPLIER", "Supplier_ID", supplierId, "Supplier");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE GOODS_RECEIPT
                    SET Receipt_Date = COALESCE(?, Receipt_Date),
                        Supplier_ID  = COALESCE(?, Supplier_ID)
                    WHERE Receipt_ID = ?
                    """)) {
                ps.setObject(1, date);
                ps.setString(2, supplierId);
                ps.setString(3, id);
                ps.executeUpdate();
            }
            if (lines != null) {
                for (StockLine line : lines) {
                    StockLedger.requireProductAndBin(conn, line.productId(), line.bin());
                }
                List<StockLine> before = stockLines(conn, id);
                deleteLines(conn, id);
                insertLines(conn, id, lines);
                try {
                    StockLedger.apply(conn, StockLine.difference(before, lines));
                } catch (ApiException e) {
                    throw explain(e, "Goods receipt " + id + " cannot be changed this way");
                }
            }
            return true;
        });
    }

    /** Takes the received stock back out, then deletes lines and header. @return false if not found */
    public boolean delete(String id) {
        return Transactions.run(dataSource, ENTITY, conn -> {
            if (!lockHeader(conn, id)) {
                return false;
            }
            try {
                StockLedger.apply(conn, StockLine.difference(stockLines(conn, id), List.of()));
            } catch (ApiException e) {
                throw explain(e, "Goods receipt " + id + " cannot be deleted because part of its stock "
                        + "has already left the bin");
            }
            deleteLines(conn, id);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GOODS_RECEIPT WHERE Receipt_ID = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            return true;
        });
    }

    // ---- helpers -------------------------------------------------------------------------

    private Optional<GoodsReceipt> findById(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_HEADERS + " WHERE gr.Receipt_ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapHeader(rs, lines(conn, id))) : Optional.empty();
            }
        }
    }

    /** SELECT ... FOR UPDATE: nobody else can change this receipt until COMMIT. */
    private static boolean lockHeader(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Receipt_ID FROM GOODS_RECEIPT WHERE Receipt_ID = ? FOR UPDATE")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertLines(Connection conn, String receiptId, List<StockLine> lines)
            throws SQLException {
        String sql = """
                INSERT INTO GOODS_RECEIPT_LINE
                    (Receipt_ID, Line_Num, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int lineNo = 1;
            for (StockLine line : lines) {
                ps.setString(1, receiptId);
                ps.setInt(2, lineNo++);
                ps.setString(3, line.productId());
                StockLedger.setBin(ps, 4, line.bin());
                ps.setInt(8, line.quantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void deleteLines(Connection conn, String receiptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GOODS_RECEIPT_LINE WHERE Receipt_ID = ?")) {
            ps.setString(1, receiptId);
            ps.executeUpdate();
        }
    }

    /** The saved lines of one receipt as stock changes. */
    private static List<StockLine> stockLines(Connection conn, String receiptId) throws SQLException {
        List<StockLine> result = new ArrayList<>();
        for (ReceiptLine line : lines(conn, receiptId)) {
            result.add(new StockLine(line.productId(), BinId.parse(line.binId()), line.quantity()));
        }
        return result;
    }

    private static List<ReceiptLine> lines(Connection conn, String receiptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES + " WHERE grl.Receipt_ID = ? ORDER BY grl.Line_Num")) {
            ps.setString(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReceiptLine> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapLine(rs));
                }
                return result;
            }
        }
    }

    private static Map<String, List<ReceiptLine>> allLines(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES + " ORDER BY grl.Receipt_ID, grl.Line_Num");
             ResultSet rs = ps.executeQuery()) {
            Map<String, List<ReceiptLine>> result = new LinkedHashMap<>();
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("Receipt_ID"), k -> new ArrayList<>()).add(mapLine(rs));
            }
            return result;
        }
    }

    private static GoodsReceipt mapHeader(ResultSet rs, List<ReceiptLine> lines) throws SQLException {
        return new GoodsReceipt(
                rs.getString("Receipt_ID"),
                IsoTime.date(rs, "Receipt_Date"),
                rs.getString("Supplier_ID"),
                rs.getString("Supplier_Name"),
                lines);
    }

    private static ReceiptLine mapLine(ResultSet rs) throws SQLException {
        return new ReceiptLine(
                rs.getInt("Line_Num"),
                rs.getString("Product_ID"),
                rs.getString("Product_Name"),
                rs.getString("Unit"),
                BinId.from(rs, "").toString(),
                rs.getInt("Quantity"));
    }

    /** "Goods receipt GR05 cannot be deleted ...: Not enough stock: bin ... has only 3 ..." */
    private static ApiException explain(ApiException e, String reason) {
        return new ApiException(e.status(), e.code(), reason + ". " + e.getMessage(), e);
    }
}
