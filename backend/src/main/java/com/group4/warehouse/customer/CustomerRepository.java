package com.group4.warehouse.customer;

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
 * All SQL for the customer table. Same rules as WarehouseRepository:
 * PreparedStatement parameters only, try-with-resources, DatabaseErrors.translate on failure.
 */
@Repository
public class CustomerRepository {

    private static final String SELECT_COLUMNS =
            "SELECT customer_id, name, phone, address FROM customer";

    private final DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q = null returns every row; otherwise matches ID or name. */
    public List<Customer> findAll(String q) {
        String sql = SELECT_COLUMNS
                + " WHERE ? IS NULL OR customer_id LIKE ? OR name LIKE ?"
                + " ORDER BY customer_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer");
        }
    }

    public Optional<Customer> findById(String id) {
        String sql = SELECT_COLUMNS + " WHERE customer_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer");
        }
    }

    public void insert(Customer customer) {
        String sql = "INSERT INTO customer (customer_id, name, phone, address)"
                + " VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.id());
            ps.setString(2, customer.name());
            ps.setString(3, customer.phone());
            ps.setString(4, customer.address());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer");
        }
    }

    /**
     * Partial update. NULL parameter = keep the current value.
     * For phone and address, "" clears the value (stored as NULL).
     * Each optional column uses its parameter twice: IF(? IS NULL, keep, NULLIF(?, '')).
     * @return false if no customer has this ID
     */
    public boolean update(String id, String name, String phone, String address) {
        String sql = "UPDATE customer"
                + " SET name    = COALESCE(?, name),"
                + "     phone   = IF(? IS NULL, phone,   NULLIF(?, '')),"
                + "     address = IF(? IS NULL, address, NULLIF(?, ''))"
                + " WHERE customer_id = ?";

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
            throw DatabaseErrors.translate(e, "customer");
        }
    }

    /** @return false if no customer has this ID */
    public boolean delete(String id) {
        String sql = "DELETE FROM customer WHERE customer_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer");
        }
    }

    private static Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getString("customer_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("address"));
    }
}
