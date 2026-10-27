package com.group4.warehouse.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/customers.
 * The phone rule is the same as the CHECK constraint ck_customer_phone in schema.sql,
 * so the user gets a clear field error before the database is even asked.
 */
public record CreateCustomerRequest(

        @NotBlank(message = "Customer ID is required.")
        @Size(max = 40, message = "Customer ID must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Customer ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Customer name is required.")
        @Size(max = 120, message = "Customer name must be at most 120 characters.")
        String name,

        // Optional: null or "" is allowed. @Pattern ignores null; "" is turned into NULL by the service.
        @Pattern(regexp = "|\\s*[0-9+() -]{6,20}\\s*",
                message = "Phone must be 6-20 characters: digits, spaces, '+', '-', '(' and ')'.")
        String phone,

        @Size(max = 250, message = "Address must be at most 250 characters.")
        String address) {
}
