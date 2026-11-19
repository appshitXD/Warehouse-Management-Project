package com.group4.warehouse.staff;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/staff/{id}. A missing field (null) is left unchanged.
 * warehouseId / supervisorId: "" removes the value (sets NULL).
 * phones: when sent, it REPLACES the whole list and must contain at least one number.
 * The ID itself cannot be changed.
 */
public record UpdateStaffRequest(

        @Size(max = 100, message = "Staff name must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Staff name must not be blank.")
        String name,

        @Size(max = 150, message = "Address must be at most 150 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Address must not be blank.")
        String address,

        @Size(max = 10, message = "Warehouse ID must be at most 10 characters.")
        String warehouseId,

        @Size(max = 10, message = "Supervisor ID must be at most 10 characters.")
        String supervisorId,

        @Size(min = 1, message = "At least one phone number is required.")
        List<@NotBlank(message = "Phone number must not be blank.")
             @Pattern(regexp = StaffRules.PHONE_PATTERN, message = StaffRules.PHONE_MESSAGE)
             String> phones) {
}
