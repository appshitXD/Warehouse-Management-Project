package com.group4.warehouse.common;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Turns a raw SQLException into a message a user can understand.
 *
 * The brief requires this: "Deleting a customer who still has orders must produce
 * a readable message, not a stack trace." Every repository calls translate(...)
 * in its catch block, so the rule is enforced in one place.
 *
 * Error numbers are MySQL's (MariaDB uses the same ones, except 4025 for CHECK).
 */
public final class DatabaseErrors {

    private static final Logger log = LoggerFactory.getLogger(DatabaseErrors.class);

    // MySQL:   Check constraint 'ck_product_reorder' is violated.
    // MariaDB: CONSTRAINT `ck_product_reorder` failed for `warehouse_db`.`product`
    private static final Pattern CHECK_NAME =
            Pattern.compile("(?:Check constraint '|CONSTRAINT `)(\\w+)");

    // Data too long for column 'warehouse_name' at row 1
    private static final Pattern COLUMN_NAME = Pattern.compile("column '(\\w+)'");

    private DatabaseErrors() {
    }

    /**
     * @param e      the exception thrown by JDBC
     * @param entity what the user was working on, e.g. "warehouse" or "bin"
     */
    public static ApiException translate(SQLException e, String entity) {
        String sqlState = e.getSQLState() == null ? "" : e.getSQLState();
        String text = e.getMessage() == null ? "" : e.getMessage();

        // 08xxx = connection problems (MySQL not running, wrong URL, ...)
        if (sqlState.startsWith("08")) {
            log.error("Cannot reach the database", e);
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                    "Cannot reach the database. Please try again later.", e);
        }

        switch (e.getErrorCode()) {
            case 1062: // duplicate primary key or UNIQUE value
                return new ApiException(HttpStatus.CONFLICT, "DUPLICATE",
                        text.contains("PRIMARY")
                                ? "A " + entity + " with this ID already exists."
                                : "A " + entity + " with this name already exists.", e);
            case 1451: // parent row still referenced by a child row
                return new ApiException(HttpStatus.CONFLICT, "IN_USE",
                        "This " + entity + " cannot be deleted because other records still use it. "
                                + "Delete or move those records first.", e);
            case 1452: // child row points at a parent that does not exist
                return new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                        "The " + entity + " refers to a record that does not exist.", e);
            case 3819: // MySQL CHECK constraint
            case 4025: // MariaDB CHECK constraint
                return new ApiException(HttpStatus.BAD_REQUEST, "CHECK_FAILED",
                        "A value breaks a database rule (" + firstGroup(CHECK_NAME, text) + ").", e);
            case 1406: // value longer than the column allows
                return new ApiException(HttpStatus.BAD_REQUEST, "TOO_LONG",
                        "The value for " + firstGroup(COLUMN_NAME, text) + " is too long.", e);
            case 1048: // NULL in a NOT NULL column
                return new ApiException(HttpStatus.BAD_REQUEST, "REQUIRED",
                        "The value for " + firstGroup(COLUMN_NAME, text) + " is required.", e);
            default:
                log.error("Unexpected database error on {}", entity, e);
                return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                        "Unexpected database error. Please try again.", e);
        }
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : "unknown";
    }
}
