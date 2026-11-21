package com.group4.warehouse.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * One line of a goods receipt, goods issue or stock count, as the form sends it:
 * { "productId": "P01", "binId": "W02|Z02|A02|B007", "quantity": 10 }
 *
 * For a stock count, quantity is the counted quantity and may be 0 (the bin was empty).
 * Receipts and issues additionally require quantity > 0 (checked in their service,
 * same rule as CHECK (Quantity > 0) on their line tables).
 */
public record DocumentLineRequest(

        @NotBlank(message = "Product is required.")
        @Size(max = 10, message = "Product ID must be at most 10 characters.")
        String productId,

        @NotBlank(message = "Bin is required.")
        String binId,

        @NotNull(message = "Quantity is required.")
        @PositiveOrZero(message = "Quantity must not be negative.")
        Integer quantity) {
}
