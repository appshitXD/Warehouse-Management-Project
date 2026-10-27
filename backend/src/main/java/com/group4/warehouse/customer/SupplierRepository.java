package com.group4.warehouse.supplier;

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
 * All SQL for the supplier table. Same rules as WarehouseRepository:
 * PreparedStatement parameters only, try-with-resources, DatabaseErrors.translate on failure.
 */
@Repository
public class SupplierRepository {

    private static final String SELECT_COLUMNS =
            "SELECT supplier_id, supplier_name, supplier_phone, supplier_address FROM supplier";

    private final DataSource dataSource;

    public SupplierRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q = null returns every row; otherwise matches ID or name. */
    public List<Supplier> findAll(String q) {
        String sql = SELECT_COLUMNS
                + " WHERE ? IS NULL OR supplier_id LIKE ? OR supplier_name LIKE ?"
                + " ORDER BY supplier_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Supplier> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "supplier");
        }
    }

    public Optional<Supplier> findById(String id) {
        String sql = SELECT_COLUMNS + " WHERE supplier_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "supplier");
        }
    }

    public void insert(Supplier supplier) {
        String sql = "INSERT INTO supplier (supplier_id, supplier_name, supplier_phone, supplier_address)"
                + " VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplier.id());
            ps.setString(2, supplier.name());
            ps.setString(3, supplier.phone());
            ps.setString(4, supplier.address());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "supplier");
        }
    }

    /**
     * Partial update. NULL parameter = keep the current value.
     * For phone and address, "" clears the value (stored as NULL).
     * Each optional column uses its parameter twice: IF(? IS NULL, keep, NULLIF(?, '')).
     * @return false if no supplier has this ID
     */
    public boolean update(String id, String name, String phone, String address) {
        String sql = "UPDATE supplier"
                + " SET supplier_name    = COALESCE(?, supplier_name),"
                + "     supplier_phone   = IF(? IS NULL, supplier_phone,   NULLIF(?, '')),"
                + "     supplier_address = IF(? IS NULL, supplier_address, NULLIF(?, ''))"
                + " WHERE supplier_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, phone);
            ps.setString(4, address);
            ps.setString(5, address);
            ps.setString(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "supplier");
        }
    }

    /** @return false if no supplier has this ID */
    public boolean delete(String id) {
        String sql = "DELETE FROM supplier WHERE supplier_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "supplier");
        }
    }

    private static Supplier map(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getString("supplier_id"),
                rs.getString("supplier_name"),
                rs.getString("supplier_phone"),
                rs.getString("supplier_address"));
    }
}
