package com.group4.warehouse.common;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Dates {

    public static final LocalDate DEFAULT_AS_OF = LocalDate.of(2026, 10, 18);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private Dates() {
    }

    public static ZoneId zone(String timezone) {
        String value = Text.trimToNull(timezone);
        if (value == null) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value);
        } catch (DateTimeException e) {
            throw ApiException.badRequest("Time zone " + value + " is not valid.");
        }
    }

    public static String iso(LocalDateTime value, ZoneId zone) {
        return value == null ? null : value.atZone(zone).format(ISO);
    }

    public static String iso(LocalDateTime value) {
        return iso(value, ZoneId.systemDefault());
    }

    public static boolean isDateOnly(String value) {
        return value != null && value.trim().matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static LocalDate asOf(String value) {
        String text = Text.trimToNull(value);
        if (text == null) {
            return DEFAULT_AS_OF;
        }
        return isDateOnly(text) ? parseDate(text, "asOf") : parseDateTime(text, ZoneId.systemDefault(), "asOf").toLocalDate();
    }

    public static LocalDateTime parseDateTime(String value, ZoneId zone, String field) {
        String text = value.trim();
        try {
            if (isDateOnly(text)) {
                return LocalDate.parse(text).atStartOfDay();
            }
            if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(text).atZoneSameInstant(zone).toLocalDateTime();
            }
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("The value of " + field + " is not a valid date. Use YYYY-MM-DD or an ISO 8601 date-time.");
        }
    }

    public static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("The value of " + field + " is not a valid date. Use YYYY-MM-DD.");
        }
    }

    public static LocalDateTime from(String value, ZoneId zone, LocalDateTime fallback) {
        String text = Text.trimToNull(value);
        return text == null ? fallback : parseDateTime(text, zone, "from");
    }

    public static LocalDateTime to(String value, ZoneId zone, LocalDateTime fallback) {
        String text = Text.trimToNull(value);
        if (text == null) {
            return fallback;
        }
        LocalDateTime result = parseDateTime(text, zone, "to");
        return isDateOnly(text) ? result.plusDays(1) : result;
    }

    public static LocalDateTime documentTime(String value) {
        String text = Text.trimToNull(value);
        return text == null ? LocalDateTime.now().withNano(0) : parseDateTime(text, ZoneId.systemDefault(), "date").withNano(0);
    }
}
