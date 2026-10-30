package com.group4.warehouse.zone;

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
public class ZoneRepository {
    private static final String SELECT_COLUMNS =
            "SELECT warehouse_id, zone_id, zone_name FROM zone";

    private final DataSource dataSource;

    public ZoneRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Zone> findAll(String warehouseId, String q) {
        String sql = SELECT_COLUMNS
                + " WHERE (? IS NULL OR warehouse_id = ?)"
                + "   AND (? IS NULL OR zone_id LIKE ? OR zone_name LIKE ?)"
                + " ORDER BY warehouse_id, zone_id";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, warehouseId);
            ps.setString(3, q);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Zone> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "zone");
        }
    }

    public Optional<Zone> findById(String warehouseId, String zoneId) {
        String sql = SELECT_COLUMNS + " WHERE warehouse_id = ? AND zone_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "zone");
        }
    }

    public void insert(Zone zone) {
        String sql = "INSERT INTO zone (zone_id, warehouse_id, zone_name) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zone.id());
            ps.setString(2, zone.warehouseId());
            ps.setString(3, zone.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "zone");
        }
    }

    public boolean update(String warehouseId, String zoneId, String name) {
        String sql = "UPDATE zone SET zone_name = ? WHERE warehouse_id = ? AND zone_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, warehouseId);
            ps.setString(3, zoneId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "zone");
        }
    }

    public boolean delete(String warehouseId, String zoneId) {
        String sql = "DELETE FROM zone WHERE warehouse_id = ? AND zone_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, zoneId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "zone");
        }
    }

    private static Zone map(ResultSet rs) throws SQLException {
        return new Zone(
                rs.getString("warehouse_id"),
                rs.getString("zone_id"),
                rs.getString("zone_name"));
    }
}
