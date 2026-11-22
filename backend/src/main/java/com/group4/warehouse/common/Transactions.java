package com.group4.warehouse.common;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs several SQL statements as ONE transaction: COMMIT if every step succeeds,
 * ROLLBACK if any step throws. Used by every save that touches more than one table
 * (a receipt and its lines and the stock in PRODUCT_BIN, for example).
 *
 *   Transactions.run(dataSource, "goods receipt", conn -> { ...statements...; return result; });
 */
public final class Transactions {

    private static final Logger log = LoggerFactory.getLogger(Transactions.class);

    /** The work done inside the transaction. It may throw SQLException or ApiException. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection conn) throws SQLException;
    }

    private Transactions() {
    }

    /**
     * @param entity what the user is saving, used in readable error messages ("goods receipt")
     */
    public static <T> T run(DataSource dataSource, String entity, Work<T> work) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);          // start the transaction
            try {
                T result = work.run(conn);
                conn.commit();                  // every change becomes visible together
                return result;
            } catch (SQLException | RuntimeException e) {   // RuntimeException includes ApiException
                rollbackQuietly(conn);          // undo every step done so far
                throw e;
            } finally {
                conn.setAutoCommit(true);       // the pooled connection goes back clean
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, entity);
        }
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error("Rollback failed", e);
        }
    }
}
