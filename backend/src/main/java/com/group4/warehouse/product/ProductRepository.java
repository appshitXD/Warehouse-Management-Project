package com.group4.warehouse.product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.DatabaseErrors;

/**
 * All SQL for the product table. Same rules as the other repositories:
 * PreparedStatement parameters only, try-with-resources, DatabaseErrors.translate on failure.
 * New here: reorder_level is INT; setObject(..., Types.INTEGER) lets the PATCH pass NULL (= keep).
 */
@Repository
public class ProductRepository {

    private static final String SELECT_COLUMNS =
            "SELECT product_id, product_name, unit, storage_requirement, reorder_level FROM product";

    private final DataSource dataSource;

    public ProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q = null returns every row; otherwise matches ID or name. */
    public List<Product> findAll(String q) {
        String sql = SELECT_COLUMNS
                + " WHERE ? IS NULL OR product_id LIKE ? OR product_name LIKE ?"
                + " ORDER BY product_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    public Optional<Product> findById(String id) {
        String sql = SELECT_COLUMNS + " WHERE product_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    public void insert(Product product) {
        String sql = "INSERT INTO product (product_id, product_name, unit, storage_requirement, reorder_level)"
                + " VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.id());
            ps.setString(2, product.name());
            ps.setString(3, product.unit());
            ps.setString(4, product.storage());
            ps.setInt(5, product.reorderLevel());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    /**
     * Partial update: COALESCE(?, column) keeps the current value when the parameter is NULL.
     * All columns are NOT NULL, so COALESCE is enough (nothing can be cleared).
     * @return false if no product has this ID
     */
    public boolean update(String id, String name, String unit, String storage, Integer reorderLevel) {
        String sql = "UPDATE product"
                + " SET product_name        = COALESCE(?, product_name),"
                + "     unit                = COALESCE(?, unit),"
                + "     storage_requirement = COALESCE(?, storage_requirement),"
                + "     reorder_level       = COALESCE(?, reorder_level)"
                + " WHERE product_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, unit);
            ps.setString(3, storage);
            ps.setObject(4, reorderLevel, Types.INTEGER);
            ps.setString(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    /** @return false if no product has this ID */
    public boolean delete(String id) {
        String sql = "DELETE FROM product WHERE product_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    private static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("product_id"),
                rs.getString("product_name"),
                rs.getString("unit"),
                rs.getString("storage_requirement"),
                rs.getInt("reorder_level"));
    }
}
