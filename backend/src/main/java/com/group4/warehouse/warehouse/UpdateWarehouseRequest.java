package com.group4.warehouse.warehouse;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/warehouses/{id}.
 * PATCH = partial update: a field that is missing (null) is left unchanged.
 * The ID is the primary key and cannot be changed here.
 */
public record UpdateWarehouseRequest(

        // null = keep the current name; if it is sent, it must not be blank.
        @Size(max = 120, message = "Warehouse name must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Warehouse name must not be blank.")
        String name,

        // null = keep; "" = clear the address.
        @Size(max = 250, message = "Address must be at most 250 characters.")
        String address) {
}
