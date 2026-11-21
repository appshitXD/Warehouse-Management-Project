package com.group4.warehouse.order;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of PATCH /api/orders/{id}. A missing field (null) is left unchanged. */
public record UpdateOrderRequest(

        String time,

        @Pattern(regexp = CustomerOrder.STATUS_PATTERN, message = CustomerOrder.STATUS_MESSAGE)
        String status,

        @Size(max = 10, message = "Customer ID must be at most 10 characters.")
        String customerId) {
}
