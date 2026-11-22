package com.group4.warehouse.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Changes to PRODUCT_BIN (the quantity of a product held in a bin), shared by
 * goods receipts (+), goods issues (-) and stock counts (= counted quantity).
 *
 * Every method takes the Connection of a transaction that is already open
 * (see Transactions.run), so the stock change and the document rows are saved
 * together or not at all.
 *
 * The rows are read with SELECT ... FOR UPDATE: they stay locked until COMMIT,
 * so two documents saved at the same moment cannot both take the same stock.
 * CHECK (Quantity >= 0) on PRODUCT_BIN is still the last guard in the database.
 */
public final class StockLedger {

    private StockLedger() {
    }

    /** Fails with 409 REFERENCE_NOT_FOUND if the product or the bin does not exist. */
    public static void requireProductAndBin(Connection conn, String productId, BinId bin)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM PRODUCT WHERE Product_ID = ?")) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                            "Product " + productId + " does not exist.");
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM BIN
                WHERE Warehouse_ID = ? AND Zone_ID = ? AND Aisle_ID = ? AND Position = ?
                """)) {
            setBin(ps, 1, bin);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                            "Bin " + bin + " does not exist.");
                }
            }
        }
    }

    /** Current quantity of the product in the bin (row locked), or 0 if the product is not there. */
    public static int quantity(Connection conn, String productId, BinId bin) throws SQLException {
        Integer current = lockRow(conn, productId, bin);
        return current == null ? 0 : current;
    }

    /** Goods receipt: put stock into a bin. Creates the PRODUCT_BIN row if needed. */
    public static void add(Connection conn, String productId, BinId bin, int quantity)
            throws SQLException {
        String sql = """
                INSERT INTO PRODUCT_BIN (Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE Quantity = Quantity + ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            setBin(ps, 2, bin);
            ps.setInt(6, quantity);
            ps.setInt(7, quantity);
            ps.executeUpdate();
        }
    }

    /**
     * Goods issue, or undoing a receipt: take stock out of a bin.
     * 409 NOT_IN_BIN if the product is not in the bin, 409 NOT_ENOUGH_STOCK if the bin holds less.
     */
    public static void subtract(Connection conn, String productId, BinId bin, int quantity)
            throws SQLException {
        Integer current = lockRow(conn, productId, bin);
        if (current == null) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_IN_BIN",
                    "Product " + productId + " is not stored in bin " + bin + ".");
        }
        if (current < quantity) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_ENOUGH_STOCK",
                    "Not enough stock: bin " + bin + " has only " + current + " of " + productId
                            + ", but " + quantity + " are needed.");
        }
        String sql = """
                UPDATE PRODUCT_BIN SET Quantity = Quantity - ?
                WHERE Product_ID = ? AND Warehouse_ID = ? AND Zone_ID = ? AND Aisle_ID = ? AND Position = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            setBin(ps, 3, bin);
            ps.executeUpdate();
        }
    }

    /**
     * Completed stock count: the counted quantity becomes the stock.
     * No row is created when the product was not in the bin and the count found none.
     */
    public static void setTo(Connection conn, String productId, BinId bin, int quantity)
            throws SQLException {
        Integer current = lockRow(conn, productId, bin);
        if (current == null && quantity == 0) {
            return;
        }
        String sql = """
                INSERT INTO PRODUCT_BIN (Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE Quantity = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            setBin(ps, 2, bin);
            ps.setInt(6, quantity);
            ps.setInt(7, quantity);
            ps.executeUpdate();
        }
    }

    /**
     * Applies stock changes from StockLine.difference / totals: positive quantities are added,
     * negative ones are taken out (and may fail with NOT_IN_BIN / NOT_ENOUGH_STOCK).
     */
    public static void apply(Connection conn, List<StockLine> changes) throws SQLException {
        for (StockLine change : changes) {
            if (change.quantity() < 0) {
                subtract(conn, change.productId(), change.bin(), -change.quantity());
            }
        }
        for (StockLine change : changes) {
            if (change.quantity() > 0) {
                add(conn, change.productId(), change.bin(), change.quantity());
            }
        }
    }

    /** Takes the given lines out of stock (a goods issue being shipped). */
    public static void takeOut(Connection conn, List<StockLine> lines) throws SQLException {
        apply(conn, StockLine.difference(lines, List.of()));
    }

    /** null = no PRODUCT_BIN row for this product and bin. */
    private static Integer lockRow(Connection conn, String productId, BinId bin) throws SQLException {
        String sql = """
                SELECT Quantity FROM PRODUCT_BIN
                WHERE Product_ID = ? AND Warehouse_ID = ? AND Zone_ID = ? AND Aisle_ID = ? AND Position = ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            setBin(ps, 2, bin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** Sets the 4 bin key columns, in the order warehouse, zone, aisle, position. */
    public static void setBin(PreparedStatement ps, int first, BinId bin) throws SQLException {
        ps.setString(first, bin.warehouseId());
        ps.setString(first + 1, bin.zoneId());
        ps.setString(first + 2, bin.aisleId());
        ps.setString(first + 3, bin.position());
    }
}
