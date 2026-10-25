package com.group4.warehouse.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/warehouses.
 * These checks run before any SQL (the brief: "Input must be validated before it
 * reaches the database"). The database CHECK constraints are the second line of defence.
 */
public record CreateWarehouseRequest(

        // The ID is typed by the user and ends up in URLs, so only safe characters.
        @NotBlank(message = "Warehouse ID is required.")
        @Size(max = 40, message = "Warehouse ID must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Warehouse ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Warehouse name is required.")
        @Size(max = 120, message = "Warehouse name must be at most 120 characters.")
        String name,

        @Size(max = 250, message = "Address must be at most 250 characters.")
        String address) {
}
