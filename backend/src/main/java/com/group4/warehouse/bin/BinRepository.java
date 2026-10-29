package com.group4.warehouse.bin;

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
public class BinRepository {
    private static final String SELECT_COLUMNS =
            "SELECT warehouse_id, zone_id, aisle_id, position, name FROM bin";

    private static final String KEY =
            " WHERE warehouse_id = ? AND zone_id = ? AND aisle_id = ? AND position = ?";

    private final DataSource dataSource;

    public BinRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Bin> findAll(String warehouseId, String zoneId, String aisleId, String q) {
        String sql = SELECT_COLUMNS
                + " WHERE (? IS NULL OR warehouse_id = ?)"
                + "   AND (? IS NULL OR zone_id = ?)"
                + "   AND (? IS NULL OR aisle_id = ?)"
                + "   AND (? IS NULL OR position LIKE ? OR name LIKE ?)"
                + " ORDER BY warehouse_id, zone_id, aisle_id, position";
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, warehouseId);
            ps.setString(3, zoneId);
            ps.setString(4, zoneId);
            ps.setString(5, aisleId);
            ps.setString(6, aisleId);
            ps.setString(7, q);
            ps.setString(8, pattern);
            ps.setString(9, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Bin> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "bin");
        }
    }

    public Optional<Bin> findById(String warehouseId, String zoneId, String aisleId, String position) {
        String sql = SELECT_COLUMNS + KEY;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setKey(ps, 1, warehouseId, zoneId, aisleId, position);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "bin");
        }
    }

    public void insert(Bin bin) {
        String sql = "INSERT INTO bin (position, aisle_id, name, zone_id, warehouse_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bin.position());
            ps.setString(2, bin.aisleId());
            ps.setString(3, bin.name());
            ps.setString(4, bin.zoneId());
            ps.setString(5, bin.warehouseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "bin");
        }
    }

    public boolean update(String warehouseId, String zoneId, String aisleId, String position, String name) {
        String sql = "UPDATE bin SET name = IF(? IS NULL, name, NULLIF(?, ''))" + KEY;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, name);
            setKey(ps, 3, warehouseId, zoneId, aisleId, position);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "bin");
        }
    }

    public boolean delete(String warehouseId, String zoneId, String aisleId, String position) {
        String sql = "DELETE FROM bin" + KEY;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setKey(ps, 1, warehouseId, zoneId, aisleId, position);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "bin");
        }
    }

    private static void setKey(PreparedStatement ps, int first, String warehouseId, String zoneId,
                               String aisleId, String position) throws SQLException {
        ps.setString(first, warehouseId);
        ps.setString(first + 1, zoneId);
        ps.setString(first + 2, aisleId);
        ps.setString(first + 3, position);
    }

    private static Bin map(ResultSet rs) throws SQLException {
        return new Bin(
                rs.getString("warehouse_id"),
                rs.getString("zone_id"),
                rs.getString("aisle_id"),
                rs.getString("position"),
                rs.getString("name"));
    }
}
