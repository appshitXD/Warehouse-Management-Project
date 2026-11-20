package com.group4.warehouse.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** contracts.js only accepts ISO 8601 with a time zone, e.g. 2026-10-12T00:00:00+07:00. */
class IsoTimeTest {

    @Test
    void dateBecomesMidnightInVietnam() {
        assertEquals("2026-10-12T00:00:00+07:00", IsoTime.of(LocalDate.of(2026, 10, 12)));
    }

    @Test
    void dateTimeKeepsItsTime() {
        assertEquals("2026-10-15T10:20:00+07:00", IsoTime.of(LocalDateTime.of(2026, 10, 15, 10, 20)));
    }

    @Test
    void nullStaysNull() {
        assertNull(IsoTime.of((LocalDate) null));
        assertNull(IsoTime.of((LocalDateTime) null));
    }
}
