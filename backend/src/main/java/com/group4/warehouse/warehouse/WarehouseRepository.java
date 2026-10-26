package com.group4.warehouse.warehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.DatabaseErrors;

/**
 * All SQL for the warehouse table lives here.
 *
 * Rules from the brief, followed in every method:
 *  - every user value goes through a PreparedStatement parameter (?), never string concatenation
 *  - Connection, PreparedStatement and ResultSet are closed with try-with-resources
 *  - SQL errors become readable messages via DatabaseErrors.translate(...)
 */
@Repository
public class WarehouseRepository {

    private static final String SELECT_COLUMNS =
            "SELECT warehouse_id, warehouse_name, address FROM warehouse";

    private final DataSource dataSource;

    public WarehouseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q = null returns every row; otherwise matches ID or name. */
    public List<Warehouse> findAll(String q) {
        String sql = SELECT_COLUMNS
                + " WHERE ? IS NULL OR warehouse_id LIKE ? OR warehouse_name LIKE ?"
                + " ORDER BY warehouse_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Warehouse> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "warehouse");
        }
    }

    public Optional<Warehouse> findById(String id) {
        String sql = SELECT_COLUMNS + " WHERE warehouse_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "warehouse");
        }
    }

    public void insert(Warehouse warehouse) {
        String sql = "INSERT INTO warehouse (warehouse_id, warehouse_name, address) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouse.id());
            ps.setString(2, warehouse.name());
            ps.setString(3, warehouse.address());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "warehouse");
        }
    }

    /**
     * Partial update. A NULL parameter means "keep the current value":
     *  - name:    COALESCE(?, warehouse_name) keeps the old name when ? is NULL
     *  - address: NULL keeps it, "" clears it (stored as NULL), anything else replaces it
     * @return false if no warehouse has this ID
     */
    public boolean update(String id, String name, String address) {
        String sql = "UPDATE warehouse"
                + " SET warehouse_name = COALESCE(?, warehouse_name),"
                + "     address        = IF(? IS NULL, address, NULLIF(?, ''))"
                + " WHERE warehouse_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, address);
            ps.setString(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "warehouse");
        }
    }

    /** @return false if no warehouse has this ID */
    public boolean delete(String id) {
        String sql = "DELETE FROM warehouse WHERE warehouse_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "warehouse");
        }
    }

    private static Warehouse map(ResultSet rs) throws SQLException {
        return new Warehouse(
                rs.getString("warehouse_id"),
                rs.getString("warehouse_name"),
                rs.getString("address"));
    }
}
