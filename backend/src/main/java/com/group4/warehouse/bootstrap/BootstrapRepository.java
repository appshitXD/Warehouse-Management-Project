package com.group4.warehouse.bootstrap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.IsoTime;

@Repository
public class BootstrapRepository {

    @FunctionalInterface
    private interface RowMapper {
        Map<String, Object> map(ResultSet rs) throws SQLException;
    }

    private final DataSource dataSource;

    public BootstrapRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Object> load() {
        try (Connection conn = dataSource.getConnection()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("warehouses", query(conn,
                    "SELECT Warehouse_ID, Warehouse_Name, Address FROM WAREHOUSE ORDER BY Warehouse_ID",
                    rs -> row("id", rs.getString("Warehouse_ID"),
                            "name", rs.getString("Warehouse_Name"),
                            "address", rs.getString("Address"))));
            result.put("zones", query(conn,
                    "SELECT Warehouse_ID, Zone_ID, Zone_Name FROM ZONE ORDER BY Warehouse_ID, Zone_ID",
                    rs -> row("warehouseId", rs.getString("Warehouse_ID"),
                            "zoneId", rs.getString("Zone_ID"),
                            "name", rs.getString("Zone_Name"))));
            result.put("aisles", query(conn,
                    "SELECT Warehouse_ID, Zone_ID, Aisle_ID, Aisle_Name FROM AISLE"
                            + " ORDER BY Warehouse_ID, Zone_ID, Aisle_ID",
                    rs -> row("warehouseId", rs.getString("Warehouse_ID"),
                            "zoneId", rs.getString("Zone_ID"),
                            "aisleId", rs.getString("Aisle_ID"),
                            "name", rs.getString("Aisle_Name"))));
            result.put("bins", query(conn,
                    "SELECT Warehouse_ID, Zone_ID, Aisle_ID, `Position`, Name FROM BIN"
                            + " ORDER BY Warehouse_ID, Zone_ID, Aisle_ID, `Position`",
                    rs -> row("id", BinId.from(rs, "").toString(),
                            "warehouseId", rs.getString("Warehouse_ID"),
                            "zone", rs.getString("Zone_ID"),
                            "aisle", rs.getString("Aisle_ID"),
                            "position", rs.getString("Position"),
                            "name", rs.getString("Name"))));
            result.put("products", query(conn,
                    "SELECT Product_ID, Product_Name, Unit, Storage_Requirement, Reorder_Level FROM PRODUCT"
                            + " ORDER BY Product_ID",
                    rs -> row("id", rs.getString("Product_ID"),
                            "name", rs.getString("Product_Name"),
                            "unit", rs.getString("Unit"),
                            "storage", rs.getString("Storage_Requirement"),
                            "reorderLevel", rs.getInt("Reorder_Level"))));
            result.put("stock", query(conn,
                    "SELECT Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`, Quantity FROM PRODUCT_BIN"
                            + " ORDER BY Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`",
                    rs -> row("productId", rs.getString("Product_ID"),
                            "binId", BinId.from(rs, "").toString(),
                            "quantity", rs.getInt("Quantity"))));
            result.put("suppliers", query(conn,
                    "SELECT Supplier_ID, Supplier_Name, Supplier_Phone, Supplier_Address FROM SUPPLIER"
                            + " ORDER BY Supplier_ID",
                    rs -> row("id", rs.getString("Supplier_ID"),
                            "name", rs.getString("Supplier_Name"),
                            "phone", rs.getString("Supplier_Phone"),
                            "address", rs.getString("Supplier_Address"))));
            result.put("customers", query(conn,
                    "SELECT Customer_ID, Name, Phone, Address FROM CUSTOMER ORDER BY Customer_ID",
                    rs -> row("id", rs.getString("Customer_ID"),
                            "name", rs.getString("Name"),
                            "phone", rs.getString("Phone"),
                            "address", rs.getString("Address"))));
            result.put("staff", staff(conn));
            result.put("orders", query(conn,
                    "SELECT Order_ID, Customer_ID, `Time`, Status FROM CUSTOMER_ORDER ORDER BY Order_ID",
                    rs -> row("id", rs.getString("Order_ID"),
                            "customerId", rs.getString("Customer_ID"),
                            "date", IsoTime.date(rs, "Time"),
                            "status", rs.getString("Status"))));
            result.put("receipts", receipts(conn));
            result.put("issues", issues(conn));
            result.put("movements", movements(conn, null, null, null));
            result.put("counts", counts(conn));
            result.put("tasks", query(conn,
                    "SELECT Task_ID, Task_Name, Staff_ID, Task_Status FROM TASK ORDER BY Task_ID",
                    rs -> row("id", rs.getString("Task_ID"),
                            "name", rs.getString("Task_Name"),
                            "staffId", rs.getString("Staff_ID"),
                            "status", rs.getString("Task_Status"))));
            return result;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "data");
        }
    }

    public static List<Map<String, Object>> movements(Connection conn, String productId,
                                                      Timestamp from, Timestamp to)
            throws SQLException {
        String sql = "SELECT Movement_ID, Movement_Date, Product_ID, Staff_ID, Quantity,"
                + " From_Warehouse_ID, From_Zone_ID, From_Aisle_ID, From_Position,"
                + " To_Warehouse_ID, To_Zone_ID, To_Aisle_ID, To_Position"
                + " FROM STOCK_MOVEMENT"
                + " WHERE (? IS NULL OR Product_ID = ?)"
                + "   AND (? IS NULL OR Movement_Date >= ?)"
                + "   AND (? IS NULL OR Movement_Date < ?)"
                + " ORDER BY Movement_Date DESC, Movement_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setString(2, productId);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, from);
            ps.setTimestamp(5, to);
            ps.setTimestamp(6, to);
            List<Map<String, Object>> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(row("id", rs.getString("Movement_ID"),
                            "date", IsoTime.date(rs, "Movement_Date"),
                            "productId", rs.getString("Product_ID"),
                            "fromBinId", BinId.from(rs, "From_").toString(),
                            "toBinId", BinId.from(rs, "To_").toString(),
                            "staffId", rs.getString("Staff_ID"),
                            "quantity", rs.getInt("Quantity")));
                }
            }
            return result;
        }
    }

    public static List<Map<String, Object>> countLines(Connection conn, String countId, boolean differencesOnly)
            throws SQLException {
        String sql = "SELECT Line_No, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`,"
                + " Recorded_Quantity, Quantity_Counted"
                + " FROM COUNT_LINE WHERE Count_ID = ?"
                + (differencesOnly ? " AND Quantity_Counted <> Recorded_Quantity" : "")
                + " ORDER BY Line_No";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, countId);
            List<Map<String, Object>> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(countLine(rs));
                }
            }
            return result;
        }
    }

    private static List<Map<String, Object>> staff(Connection conn) throws SQLException {
        Map<String, List<String>> phones = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Staff_ID, Phone FROM STAFF_PHONE ORDER BY Staff_ID, Phone");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                phones.computeIfAbsent(rs.getString("Staff_ID"), k -> new ArrayList<>()).add(rs.getString("Phone"));
            }
        }
        return query(conn,
                "SELECT Staff_ID, Staff_Name, Staff_Address, Warehouse_ID, Supervisor_ID FROM STAFF ORDER BY Staff_ID",
                rs -> row("id", rs.getString("Staff_ID"),
                        "name", rs.getString("Staff_Name"),
                        "address", rs.getString("Staff_Address"),
                        "warehouseId", rs.getString("Warehouse_ID"),
                        "supervisorId", rs.getString("Supervisor_ID"),
                        "phones", phones.getOrDefault(rs.getString("Staff_ID"), List.of())));
    }

    private static List<Map<String, Object>> receipts(Connection conn) throws SQLException {
        Map<String, List<Map<String, Object>>> lines = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Receipt_ID, Line_Num, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`, Quantity"
                        + " FROM GOODS_RECEIPT_LINE ORDER BY Receipt_ID, Line_Num");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.computeIfAbsent(rs.getString("Receipt_ID"), k -> new ArrayList<>()).add(row(
                        "lineNo", rs.getInt("Line_Num"),
                        "productId", rs.getString("Product_ID"),
                        "binId", BinId.from(rs, "").toString(),
                        "quantity", rs.getInt("Quantity")));
            }
        }
        return query(conn,
                "SELECT Receipt_ID, Receipt_Date, Supplier_ID FROM GOODS_RECEIPT ORDER BY Receipt_Date DESC, Receipt_ID",
                rs -> row("id", rs.getString("Receipt_ID"),
                        "date", IsoTime.date(rs, "Receipt_Date"),
                        "supplierId", rs.getString("Supplier_ID"),
                        "lines", lines.getOrDefault(rs.getString("Receipt_ID"), List.of())));
    }

    private static List<Map<String, Object>> issues(Connection conn) throws SQLException {
        Map<String, List<Map<String, Object>>> lines = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Issue_ID, Issue_Line_ID, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`, Quantity"
                        + " FROM GOODS_ISSUE_LINE ORDER BY Issue_ID, Issue_Line_ID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.computeIfAbsent(rs.getString("Issue_ID"), k -> new ArrayList<>()).add(row(
                        "id", rs.getInt("Issue_Line_ID"),
                        "productId", rs.getString("Product_ID"),
                        "binId", BinId.from(rs, "").toString(),
                        "quantity", rs.getInt("Quantity")));
            }
        }
        return query(conn,
                "SELECT Issue_ID, Issue_Date, Order_ID, Status, State_Of_Good FROM GOODS_ISSUE"
                        + " ORDER BY Issue_Date DESC, Issue_ID",
                rs -> row("id", rs.getString("Issue_ID"),
                        "date", IsoTime.date(rs, "Issue_Date"),
                        "orderId", rs.getString("Order_ID"),
                        "status", rs.getString("Status"),
                        "stateOfGood", rs.getString("State_Of_Good"),
                        "lines", lines.getOrDefault(rs.getString("Issue_ID"), List.of())));
    }

    private static List<Map<String, Object>> counts(Connection conn) throws SQLException {
        Map<String, List<Map<String, Object>>> lines = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Count_ID, Line_No, Product_ID, Warehouse_ID, Zone_ID, Aisle_ID, `Position`,"
                        + " Recorded_Quantity, Quantity_Counted FROM COUNT_LINE ORDER BY Count_ID, Line_No");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.computeIfAbsent(rs.getString("Count_ID"), k -> new ArrayList<>()).add(countLine(rs));
            }
        }
        return query(conn,
                "SELECT Count_ID, Count_Date, Count_Status FROM STOCK_COUNT ORDER BY Count_Date DESC, Count_ID",
                rs -> row("id", rs.getString("Count_ID"),
                        "date", IsoTime.date(rs, "Count_Date"),
                        "status", rs.getString("Count_Status"),
                        "lines", lines.getOrDefault(rs.getString("Count_ID"), List.of())));
    }

    private static Map<String, Object> countLine(ResultSet rs) throws SQLException {
        return row("lineNo", rs.getInt("Line_No"),
                "productId", rs.getString("Product_ID"),
                "binId", BinId.from(rs, "").toString(),
                "recordedQuantity", rs.getInt("Recorded_Quantity"),
                "countedQuantity", rs.getInt("Quantity_Counted"));
    }

    private static List<Map<String, Object>> query(Connection conn, String sql, RowMapper mapper) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapper.map(rs));
            }
            return result;
        }
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }
}
