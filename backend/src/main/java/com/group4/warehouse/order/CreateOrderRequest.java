package com.group4.warehouse.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/orders: { "id": "CO31", "customerId": "C05", "status": "Pending" }
 * id, time and status are optional: next COxx, now, and the column DEFAULT 'Pending'.
 */
public record CreateOrderRequest(

        @Size(max = 10, message = "Order ID must be at most 10 characters.")
        @Pattern(regexp = "|\\s*[A-Za-z0-9_-]+\\s*",
                message = "Order ID may contain only letters, digits, '-' and '_'.")
        String id,

        String time,

        @Pattern(regexp = CustomerOrder.STATUS_PATTERN, message = CustomerOrder.STATUS_MESSAGE)
        String status,

        @NotBlank(message = "Customer is required.")
        @Size(max = 10, message = "Customer ID must be at most 10 characters.")
        String customerId) {
}
