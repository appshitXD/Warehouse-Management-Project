package com.group4.warehouse.movement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/movements:
 * { "productId": "P01", "fromBinId": "W02|Z02|A02|B007", "toBinId": "W03|Z01|A02|B007",
 *   "quantity": 10, "staffId": "ST01" }
 * The bin IDs are checked by BinId.parse in the service (format warehouse|zone|aisle|position).
 */
public record CreateMovementRequest(

        @NotBlank(message = "Product is required.")
        @Size(max = 10, message = "Product ID must be at most 10 characters.")
        String productId,

        @NotBlank(message = "Source bin is required.")
        String fromBinId,

        @NotBlank(message = "Destination bin is required.")
        String toBinId,

        // Integer, not int: a missing quantity is reported as "required" instead of becoming 0.
        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than 0.")
        Integer quantity,

        @NotBlank(message = "Staff member is required.")
        @Size(max = 10, message = "Staff ID must be at most 10 characters.")
        String staffId) {
}
