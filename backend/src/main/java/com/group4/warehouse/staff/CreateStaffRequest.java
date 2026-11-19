package com.group4.warehouse.staff;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/staff. Sizes follow STAFF / STAFF_PHONE in warehouse.sql.
 * warehouseId and supervisorId are optional: null or "" means none.
 */
public record CreateStaffRequest(

        @NotBlank(message = "Staff ID is required.")
        @Size(max = 10, message = "Staff ID must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Staff ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Staff name is required.")
        @Size(max = 100, message = "Staff name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Address is required.")
        @Size(max = 150, message = "Address must be at most 150 characters.")
        String address,

        @Size(max = 10, message = "Warehouse ID must be at most 10 characters.")
        String warehouseId,

        @Size(max = 10, message = "Supervisor ID must be at most 10 characters.")
        String supervisorId,

        @NotNull(message = "At least one phone number is required.")
        @Size(min = 1, message = "At least one phone number is required.")
        List<@NotBlank(message = "Phone number must not be blank.")
             @Pattern(regexp = StaffRules.PHONE_PATTERN, message = StaffRules.PHONE_MESSAGE)
             String> phones) {
}
