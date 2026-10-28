package com.group4.warehouse.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/products/{id}. A missing field (null) is left unchanged.
 * Every product column is NOT NULL, so no field can be cleared, only replaced.
 */
public record UpdateProductRequest(

        @Size(max = 120, message = "Product name must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Product name must not be blank.")
        String name,

        @Size(max = 20, message = "Unit must be at most 20 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Unit must not be blank.")
        String unit,

        @Size(max = 120, message = "Storage requirement must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Storage requirement must not be blank.")
        String storage,

        // Integer: a fraction such as 12.5 is rejected as invalid JSON (see application.properties).
        @Min(value = 0, message = "Reorder level cannot be negative.")
        Integer reorderLevel) {
}
