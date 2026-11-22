package com.group4.warehouse.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Next ID for a document when the form does not send one: GR15 -> GR16, CO30 -> CO31.
 * Used for goods receipts (GR), goods issues (GI), stock counts (SC), orders (CO) and tasks (T).
 */
public final class DocumentIds {

    private DocumentIds() {
    }

    /**
     * Must run inside a transaction: FOR UPDATE locks the rows it reads until COMMIT,
     * so two documents saved at the same moment cannot get the same new ID.
     *
     * The table and column names are constants from our own code, never user input,
     * so they are safe to put in the SQL text. The prefix is still sent as a parameter.
     */
    public static String next(Connection conn, String table, String idColumn, String prefix)
            throws SQLException {
        String sql = "SELECT " + idColumn + " FROM " + table
                + " WHERE " + idColumn + " REGEXP CONCAT('^', ?, '[0-9]+$')"
                + " ORDER BY CAST(SUBSTRING(" + idColumn + ", ?) AS UNSIGNED) DESC"
                + " LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix);
            ps.setInt(2, prefix.length() + 1);
            try (ResultSet rs = ps.executeQuery()) {
                int last = rs.next() ? Integer.parseInt(rs.getString(1).substring(prefix.length())) : 0;
                return String.format("%s%02d", prefix, last + 1);
            }
        }
    }
}
