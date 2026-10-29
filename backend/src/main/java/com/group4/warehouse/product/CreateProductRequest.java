package com.group4.warehouse.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/products.
 * Sizes and rules follow the PRODUCT table in the report:
 * Product_ID CHAR(10), Product_Name VARCHAR(100), Unit VARCHAR(20),
 * Storage_Requirement VARCHAR(50) IN ('Ambient', 'Dry Ambient', 'Cold', 'Hazardous'),
 * Reorder_Level INT NOT NULL, CHECK (Reorder_Level > 0).
 */
public record CreateProductRequest(

        @NotBlank(message = "Product ID is required.")
        @Size(max = 10, message = "Product ID must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Product ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Product name is required.")
        @Size(max = 100, message = "Product name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Unit is required.")
        @Size(max = 20, message = "Unit must be at most 20 characters.")
        String unit,

        @NotBlank(message = "Storage requirement is required.")
        @Pattern(regexp = ProductRules.STORAGE_PATTERN, message = ProductRules.STORAGE_MESSAGE)
        String storage,

        // Integer: a fraction such as 12.5 is rejected as invalid JSON (see application.properties).
        @NotNull(message = "Reorder level is required.")
        @Positive(message = "Reorder level must be greater than 0.")
        Integer reorderLevel) {
}
