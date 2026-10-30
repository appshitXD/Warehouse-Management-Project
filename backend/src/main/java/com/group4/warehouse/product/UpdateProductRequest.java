package com.group4.warehouse.product;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/products/{id}. A missing field (null) is left unchanged.
 * Every product column is NOT NULL, so no field can be cleared, only replaced.
 * Same limits as CreateProductRequest (PRODUCT table in the report).
 */
public record UpdateProductRequest(

        @Size(max = 100, message = "Product name must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Product name must not be blank.")
        String name,

        @Size(max = 20, message = "Unit must be at most 20 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Unit must not be blank.")
        String unit,

        @Pattern(regexp = ProductRules.STORAGE_PATTERN, message = ProductRules.STORAGE_MESSAGE)
        String storage,

        // Integer: a fraction such as 12.5 is rejected as invalid JSON (see application.properties).
        @Positive(message = "Reorder level must be greater than 0.")
        Integer reorderLevel) {
}
