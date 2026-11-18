package com.group4.warehouse.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Dates and times for the API.
 *
 * MySQL DATE / DATETIME columns have no time zone. contracts.js only accepts
 * ISO 8601 with a zone, e.g. "2026-10-12T00:00:00+07:00", so every value is
 * read as warehouse local time (Vietnam, UTC+7) and printed with its offset.
 */
public final class IsoTime {

    /** The time zone the warehouse data is recorded in. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private IsoTime() {
    }

    /** DATETIME 2026-10-15 10:20:00 -> "2026-10-15T10:20:00+07:00". */
    public static String of(LocalDateTime value) {
        return value == null ? null : value.atZone(ZONE).format(FORMAT);
    }

    /** DATE 2026-10-12 -> "2026-10-12T00:00:00+07:00" (start of that day). */
    public static String of(LocalDate value) {
        return value == null ? null : value.atStartOfDay(ZONE).format(FORMAT);
    }

    /** Reads a DATETIME column without any time zone conversion by the driver. */
    public static String dateTime(ResultSet rs, String column) throws SQLException {
        return of(rs.getObject(column, LocalDateTime.class));
    }

    /** Reads a DATE column. */
    public static String date(ResultSet rs, String column) throws SQLException {
        return of(rs.getObject(column, LocalDate.class));
    }

    /** The current warehouse local time, to store in a DATETIME column. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE).withNano(0);
    }
}
