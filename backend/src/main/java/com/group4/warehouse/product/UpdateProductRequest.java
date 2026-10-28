package com.group4.warehouse.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

        @DecimalMin(value = "0", message = "Reorder level cannot be negative.")
        @Digits(integer = 9, fraction = 3,
                message = "Reorder level allows at most 9 digits before and 3 after the decimal point.")
        BigDecimal reorderLevel) {
}
