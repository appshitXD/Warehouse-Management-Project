package com.group4.warehouse.common;

/** Small helpers for cleaning user-typed text before it reaches the database. */
public final class Text {

    private Text() {
    }

    /** "  WH01 " -> "WH01"; null stays null. */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Optional fields: blank or missing becomes NULL in the database. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
