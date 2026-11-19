package com.group4.warehouse.common;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bin is identified in the database by four columns (composite key):
 * Warehouse_ID, Zone_ID, Aisle_ID, Position.
 * The frontend (contracts.js) uses one string for it: "W02|Z02|A02|B007".
 *
 * This record converts between the two forms so every feature uses the same format.
 */
public record BinId(String warehouseId, String zoneId, String aisleId, String position) {

    private static final String SEPARATOR = "|";

    /** Same as parse(value, "Bin"). */
    public static BinId parse(String value) {
        return parse(value, "Bin");
    }

    /** "W02|Z02|A02|B007" -> BinId. Throws 400 with a readable message if the format is wrong. */
    public static BinId parse(String value, String field) {
        String[] parts = value == null ? new String[0] : value.trim().split("\\|", -1);
        if (parts.length != 4) {
            throw ApiException.badRequest(field + " must look like W02|Z02|A02|B007 "
                    + "(warehouse|zone|aisle|position).");
        }
        for (String part : parts) {
            if (part.isBlank()) {
                throw ApiException.badRequest(field + " has an empty part: " + value + ".");
            }
        }
        return new BinId(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }

    /**
     * Reads the four key columns of a row, e.g. prefix "" for Warehouse_ID...,
     * or prefix "From_" for From_Warehouse_ID... in STOCK_MOVEMENT.
     */
    public static BinId from(ResultSet rs, String prefix) throws SQLException {
        return new BinId(
                rs.getString(prefix + "Warehouse_ID"),
                rs.getString(prefix + "Zone_ID"),
                rs.getString(prefix + "Aisle_ID"),
                rs.getString(prefix + "Position"));
    }

    @Override
    public String toString() {
        return warehouseId + SEPARATOR + zoneId + SEPARATOR + aisleId + SEPARATOR + position;
    }
}
