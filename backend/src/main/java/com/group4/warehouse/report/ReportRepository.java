package com.group4.warehouse.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.bootstrap.BootstrapRepository;
import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.IsoTime;

@Repository
public class ReportRepository {
    private final DataSource dataSource;

    public ReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean productExists(String productId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM PRODUCT WHERE Product_ID = ?")) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product");
        }
    }

    public List<Map<String, Object>> productLocations(String productId) {
        String sql = "SELECT pb.Product_ID, p.Product_Name, p.Unit,"
                + "       pb.Warehouse_ID, w.Warehouse_Name, pb.Zone_ID, pb.Aisle_ID, pb.`Position`, b.Name AS Bin_Name,"
                + "       pb.Quantity"
                + " FROM PRODUCT_BIN pb"
                + " JOIN PRODUCT p   ON p.Product_ID = pb.Product_ID"
                + " JOIN BIN b       ON b.`Position` = pb.`Position` AND b.Aisle_ID = pb.Aisle_ID"
                + "                 AND b.Zone_ID = pb.Zone_ID AND b.Warehouse_ID = pb.Warehouse_ID"
                + " JOIN WAREHOUSE w ON w.Warehouse_ID = pb.Warehouse_ID"
                + " WHERE pb.Product_ID = ? AND pb.Quantity > 0"
                + " ORDER BY pb.Warehouse_ID, pb.Zone_ID, pb.Aisle_ID, pb.`Position`";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            List<Map<String, Object>> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", rs.getString("Product_ID"));
                    row.put("productName", rs.getString("Product_Name"));
                    row.put("unit", rs.getString("Unit"));
                    row.put("binId", BinId.from(rs, "").toString());
                    row.put("warehouseId", rs.getString("Warehouse_ID"));
                    row.put("warehouseName", rs.getString("Warehouse_Name"));
                    row.put("zoneId", rs.getString("Zone_ID"));
                    row.put("aisleId", rs.getString("Aisle_ID"));
                    row.put("position", rs.getString("Position"));
                    row.put("binName", rs.getString("Bin_Name"));
                    row.put("quantity", rs.getInt("Quantity"));
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "report");
        }
    }

    public List<Map<String, Object>> lowStock() {
        String sql = "SELECT p.Product_ID, p.Product_Name, p.Unit, p.Reorder_Level,"
                + "       COALESCE(SUM(pb.Quantity), 0) AS Total_Quantity"
                + " FROM PRODUCT p"
                + " LEFT JOIN PRODUCT_BIN pb ON pb.Product_ID = p.Product_ID"
                + " GROUP BY p.Product_ID, p.Product_Name, p.Unit, p.Reorder_Level"
                + " HAVING COALESCE(SUM(pb.Quantity), 0) < p.Reorder_Level"
                + " ORDER BY Total_Quantity / p.Reorder_Level, p.Product_ID";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", rs.getString("Product_ID"));
                row.put("productName", rs.getString("Product_Name"));
                row.put("unit", rs.getString("Unit"));
                row.put("totalQuantity", rs.getInt("Total_Quantity"));
                row.put("reorderLevel", rs.getInt("Reorder_Level"));
                result.add(row);
            }
            return result;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "report");
        }
    }

    public List<Map<String, Object>> movements(String productId, LocalDateTime from, LocalDateTime to) {
        try (Connection conn = dataSource.getConnection()) {
            return BootstrapRepository.movements(conn, productId, Timestamp.valueOf(from), Timestamp.valueOf(to));
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "report");
        }
    }

    public List<Map<String, Object>> emptyBins(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT b.Warehouse_ID, b.Zone_ID, b.Aisle_ID, b.`Position`, b.Name"
                + " FROM BIN b"
                + " WHERE NOT EXISTS ("
                + "         SELECT 1 FROM STOCK_HISTORY h"
                + "         WHERE h.`Position` = b.`Position` AND h.Aisle_ID = b.Aisle_ID"
                + "           AND h.Zone_ID = b.Zone_ID AND h.Warehouse_ID = b.Warehouse_ID"
                + "           AND h.Quantity > 0"
                + "           AND h.Record_Date >= ? AND h.Record_Date < ?)"
                + "   AND NOT EXISTS ("
                + "         SELECT 1 FROM STOCK_HISTORY h"
                + "         WHERE h.`Position` = b.`Position` AND h.Aisle_ID = b.Aisle_ID"
                + "           AND h.Zone_ID = b.Zone_ID AND h.Warehouse_ID = b.Warehouse_ID"
                + "           AND h.Quantity > 0"
                + "           AND h.Record_Date = ("
                + "                 SELECT MAX(h2.Record_Date) FROM STOCK_HISTORY h2"
                + "                 WHERE h2.Product_ID = h.Product_ID AND h2.`Position` = h.`Position`"
                + "                   AND h2.Aisle_ID = h.Aisle_ID AND h2.Zone_ID = h.Zone_ID"
                + "                   AND h2.Warehouse_ID = h.Warehouse_ID AND h2.Record_Date < ?))"
                + " ORDER BY b.Warehouse_ID, b.Zone_ID, b.Aisle_ID, b.`Position`";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setTimestamp(3, Timestamp.valueOf(from));
            List<Map<String, Object>> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("binId", BinId.from(rs, "").toString());
                    row.put("warehouseId", rs.getString("Warehouse_ID"));
                    row.put("zoneId", rs.getString("Zone_ID"));
                    row.put("aisleId", rs.getString("Aisle_ID"));
                    row.put("position", rs.getString("Position"));
                    row.put("name", rs.getString("Name"));
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "report");
        }
    }

    public Optional<Map<String, Object>> latestCountDifferences(LocalDateTime before) {
        String sql = "SELECT Count_ID, Count_Date, Count_Status FROM STOCK_COUNT"
                + " WHERE Count_Status = 'Completed' AND Count_Date < ?"
                + " ORDER BY Count_Date DESC, Count_ID DESC LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(before));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String countId = rs.getString("Count_ID");
                Map<String, Object> count = new LinkedHashMap<>();
                count.put("id", countId);
                count.put("date", IsoTime.date(rs, "Count_Date"));
                count.put("status", rs.getString("Count_Status"));
                count.put("lines", BootstrapRepository.countLines(conn, countId, true));
                return Optional.of(count);
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "report");
        }
    }
}
