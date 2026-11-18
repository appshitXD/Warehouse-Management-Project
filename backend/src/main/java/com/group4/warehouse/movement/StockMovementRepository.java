package com.group4.warehouse.movement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.IsoTime;

/**
 * Moving stock from one bin to another touches three rows, so it runs as ONE transaction:
 *
 *   1. UPDATE PRODUCT_BIN   subtract the quantity from the source bin
 *   2. INSERT PRODUCT_BIN   add it to the destination bin
 *      ... ON DUPLICATE KEY UPDATE   (or increase the row if the product is already there)
 *   3. INSERT STOCK_MOVEMENT   record who moved what, with the next ID SMxx
 *   4. COMMIT, or ROLLBACK on any error
 *
 * Either all three changes are saved or none: stock can never disappear from the
 * source bin without arriving in the destination bin.
 */
@Repository
public class StockMovementRepository {

    private static final Logger log = LoggerFactory.getLogger(StockMovementRepository.class);

    private static final int CHECK_VIOLATED = 3819;      // CHECK constraint failed (MySQL)
    private static final int NO_PARENT_ROW = 1452;       // foreign key points at a missing row
    private static final int DUPLICATE_KEY = 1062;

    private final DataSource dataSource;

    public StockMovementRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public StockMovement move(String productId, BinId from, BinId to, int quantity, String staffId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);          // start the transaction
            try {
                subtractFromSource(conn, productId, from, quantity);
                addToDestination(conn, productId, to, quantity);
                String id = nextMovementId(conn);
                LocalDateTime now = IsoTime.now();
                insertMovement(conn, id, now, productId, from, to, quantity, staffId);

                conn.commit();                  // all three changes become visible together
                return new StockMovement(id, IsoTime.of(now), productId,
                        from.toString(), to.toString(), staffId, quantity);
            } catch (SQLException | RuntimeException e) {   // RuntimeException includes ApiException
                rollbackQuietly(conn);          // undo every step done so far
                throw e;
            } finally {
                conn.setAutoCommit(true);       // the pooled connection goes back clean
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "stock movement");
        }
    }

    /** Step 1. 0 rows = the product is not in that bin. CHECK (Quantity >= 0) = not enough stock. */
    private void subtractFromSource(Connection conn, String productId, BinId from, int quantity)
            throws SQLException {
        String sql = """
                UPDATE PRODUCT_BIN
                SET Quantity = Quantity - ?
                WHERE Product_ID = ? AND Warehouse_ID = ? AND Zone_ID = ? AND Aisle_ID = ? AND Position = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            setBin(ps, 3, from);
            if (ps.executeUpdate() == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "NOT_IN_BIN",
                        "Product " + productId + " is not stored in bin " + from + ".");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == CHECK_VIOLATED) {
                int available = currentQuantity(conn, productId, from);
                throw new ApiException(HttpStatus.CONFLICT, "NOT_ENOUGH_STOCK",
                        "Not enough stock: bin " + from + " has only " + available + " of " + productId
                                + ", cannot move " + quantity + ".", e);
            }
            throw e;
        }
    }

    /** Step 2. The primary key of PRODUCT_BIN is (product, bin), so a second row is never created. */
    private void addToDestination(Connection conn, String productId, BinId to, int quantity)
            throws SQLException {
        String sql = """
                INSERT INTO PRODUCT_BIN (Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, Position, Quantity)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE Quantity = Quantity + ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            setBin(ps, 2, to);
            ps.setInt(6, quantity);
            ps.setInt(7, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == NO_PARENT_ROW) {
                throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                        "Destination bin " + to + " does not exist.", e);
            }
            throw e;
        }
    }

    /**
     * Step 3a. SM38 -> SM39. FOR UPDATE locks the rows it reads until COMMIT, so two
     * movements saved at the same moment cannot both get the same new ID.
     */
    private String nextMovementId(Connection conn) throws SQLException {
        String sql = """
                SELECT Movement_ID FROM STOCK_MOVEMENT
                WHERE Movement_ID REGEXP '^SM[0-9]+$'
                ORDER BY CAST(SUBSTRING(Movement_ID, 3) AS UNSIGNED) DESC
                LIMIT 1
                FOR UPDATE
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int last = rs.next() ? Integer.parseInt(rs.getString(1).substring(2)) : 0;
            return String.format("SM%02d", last + 1);
        }
    }

    /** Step 3b. */
    private void insertMovement(Connection conn, String id, LocalDateTime date, String productId,
                                BinId from, BinId to, int quantity, String staffId) throws SQLException {
        String sql = """
                INSERT INTO STOCK_MOVEMENT
                    (Movement_ID, Quantity, Movement_Date, Staff_ID, Product_ID,
                     From_Position, From_Aisle_ID, From_Zone_ID, From_Warehouse_ID,
                     To_Position, To_Aisle_ID, To_Zone_ID, To_Warehouse_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setInt(2, quantity);
            ps.setObject(3, date);
            ps.setString(4, staffId);
            ps.setString(5, productId);
            ps.setString(6, from.position());
            ps.setString(7, from.aisleId());
            ps.setString(8, from.zoneId());
            ps.setString(9, from.warehouseId());
            ps.setString(10, to.position());
            ps.setString(11, to.aisleId());
            ps.setString(12, to.zoneId());
            ps.setString(13, to.warehouseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == NO_PARENT_ROW && e.getMessage().contains("FK_SM_STAFF")) {
                throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                        "Staff member " + staffId + " does not exist.", e);
            }
            if (e.getErrorCode() == DUPLICATE_KEY) {
                throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE",
                        "Another movement was saved at the same time. Please try again.", e);
            }
            throw e;
        }
    }

    private int currentQuantity(Connection conn, String productId, BinId bin) throws SQLException {
        String sql = """
                SELECT Quantity FROM PRODUCT_BIN
                WHERE Product_ID = ? AND Warehouse_ID = ? AND Zone_ID = ? AND Aisle_ID = ? AND Position = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            setBin(ps, 2, bin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Sets the 4 bin key columns, in the order warehouse, zone, aisle, position. */
    private static void setBin(PreparedStatement ps, int first, BinId bin) throws SQLException {
        ps.setString(first, bin.warehouseId());
        ps.setString(first + 1, bin.zoneId());
        ps.setString(first + 2, bin.aisleId());
        ps.setString(first + 3, bin.position());
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error("Rollback failed", e);
        }
    }
}
