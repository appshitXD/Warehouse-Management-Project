package com.group4.warehouse.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Checks that a record a form points at exists (supplier of a receipt, order of an issue, ...),
 * so the user reads "Supplier S99 does not exist." instead of a foreign key error.
 * The foreign keys in warehouse.sql still enforce the same rule in the database.
 */
public final class References {

    private References() {
    }

    /**
     * @param table  table name, a constant from our code (never user input)
     * @param column its ID column, also a constant
     * @param label  how the user calls it, e.g. "Supplier"
     */
    public static void require(Connection conn, String table, String column, String id, String label)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM " + table + " WHERE " + column + " = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw ApiException.missingReference(label + " " + id + " does not exist.");
                }
            }
        }
    }
}
