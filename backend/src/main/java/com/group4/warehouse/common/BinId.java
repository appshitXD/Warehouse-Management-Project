package com.group4.warehouse.common;

import java.sql.ResultSet;
import java.sql.SQLException;

public record BinId(String warehouseId, String zoneId, String aisleId, String position) {

    public static BinId from(ResultSet rs, String prefix) throws SQLException {
        return new BinId(
                rs.getString(prefix + "Warehouse_ID"),
                rs.getString(prefix + "Zone_ID"),
                rs.getString(prefix + "Aisle_ID"),
                rs.getString(prefix + "Position"));
    }

    public static BinId parse(String value) {
        String[] parts = value == null ? new String[0] : value.trim().split("\\|", -1);
        if (parts.length != 4 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank() || parts[3].isBlank()) {
            throw ApiException.badRequest("Bin " + value + " is not valid. Use warehouse|zone|aisle|position, for example W01|Z01|A01|B001.");
        }
        return new BinId(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }

    @Override
    public String toString() {
        return warehouseId + "|" + zoneId + "|" + aisleId + "|" + position;
    }
}
