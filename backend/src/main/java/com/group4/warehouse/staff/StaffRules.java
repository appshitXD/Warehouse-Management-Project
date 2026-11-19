package com.group4.warehouse.staff;

/** Constants shared by the staff request classes. */
final class StaffRules {

    /** STAFF_PHONE.Phone is VARCHAR(20); seed data looks like "555-0301". */
    static final String PHONE_PATTERN = "\\s*[0-9+() -]{6,20}\\s*";

    static final String PHONE_MESSAGE =
            "Phone must be 6-20 characters: digits, spaces, '+', '-', '(' and ')'.";

    private StaffRules() {
    }
}
