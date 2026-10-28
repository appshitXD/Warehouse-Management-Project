package com.group4.warehouse.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/products.
 * reorderLevel follows the column type DECIMAL(12,3) and the CHECK ck_product_reorder (>= 0).
 * If it is missing, the database DEFAULT 0 applies (see ProductService).
 */
public record CreateProductRequest(

        @NotBlank(message = "Product ID is required.")
        @Size(max = 40, message = "Product ID must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Product ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Product name is required.")
        @Size(max = 120, message = "Product name must be at most 120 characters.")
        String name,

        @NotBlank(message = "Unit is required.")
        @Size(max = 20, message = "Unit must be at most 20 characters.")
        String unit,

        @NotBlank(message = "Storage requirement is required.")
        @Size(max = 120, message = "Storage requirement must be at most 120 characters.")
        String storage,

        @DecimalMin(value = "0", message = "Reorder level cannot be negative.")
        @Digits(integer = 9, fraction = 3,
                message = "Reorder level allows at most 9 digits before and 3 after the decimal point.")
        BigDecimal reorderLevel) {
}
