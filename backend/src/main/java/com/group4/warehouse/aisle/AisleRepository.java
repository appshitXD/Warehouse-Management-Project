package com.group4.warehouse.aisle;

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

@Repository
public class AisleRepository {
    private static final String SELECT_COLUMNS =
            "SELECT warehouse_id, zone_id, aisle_id, aisle_name FROM aisle";

    private final DataSource dataSource;

    public AisleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Aisle> findAll(String warehouseId, String zoneId, String q) {
        String sql = SELECT_COLUMNS
                + " WHERE (? IS NULL OR warehouse_id = ?)"
                + "   AND (? IS NULL OR zone_id = ?)"
                + "   AND (? IS NULL OR aisle_id LIKE ? OR aisle_name LIKE ?)"
                + " ORDER BY warehouse_id, zone_id, aisle_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, warehouseId);
            ps.setString(3, zoneId);
            ps.setString(4, zoneId);
            ps.setString(5, q);
            ps.setString(6, pattern);
            ps.setString(7, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Aisle> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "aisle");
        }
    }

    public Optional<Aisle> findById(String warehouseId, String zoneId, String aisleId) {
        String sql = SELECT_COLUMNS + " WHERE warehouse_id = ? AND zone_id = ? AND aisle_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, zoneId);
            ps.setString(3, aisleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "aisle");
        }
    }

    public void insert(Aisle aisle) {
        String sql = "INSERT INTO aisle (aisle_id, aisle_name, zone_id, warehouse_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, aisle.id());
            ps.setString(2, aisle.name());
            ps.setString(3, aisle.zoneId());
            ps.setString(4, aisle.warehouseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "aisle");
        }
    }

    public boolean update(String warehouseId, String zoneId, String aisleId, String name) {
        String sql = "UPDATE aisle SET aisle_name = ?"
                + " WHERE warehouse_id = ? AND zone_id = ? AND aisle_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, warehouseId);
            ps.setString(3, zoneId);
            ps.setString(4, aisleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "aisle");
        }
    }

    public boolean delete(String warehouseId, String zoneId, String aisleId) {
        String sql = "DELETE FROM aisle WHERE warehouse_id = ? AND zone_id = ? AND aisle_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, zoneId);
            ps.setString(3, aisleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "aisle");
        }
    }

    private static Aisle map(ResultSet rs) throws SQLException {
        return new Aisle(
                rs.getString("warehouse_id"),
                rs.getString("zone_id"),
                rs.getString("aisle_id"),
                rs.getString("aisle_name"));
    }
}
