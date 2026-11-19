package com.group4.warehouse.staff;

import java.util.List;

/**
 * A staff member: one row of STAFF plus all of its rows in STAFF_PHONE.
 * STAFF_PHONE is a separate table because one person can have several phones
 * (a multi-valued attribute in the ERD).
 *
 * warehouseId and supervisorId may be null. phone is the first phone, for screens
 * that show only one; phones is the full list (never null).
 */
public record Staff(
        String id,
        String name,
        String address,
        String warehouseId,
        String supervisorId,
        String phone,
        List<String> phones) {

    public Staff(String id, String name, String address, String warehouseId, String supervisorId,
                 List<String> phones) {
        this(id, name, address, warehouseId, supervisorId,
                phones.isEmpty() ? null : phones.get(0), List.copyOf(phones));
    }
}
