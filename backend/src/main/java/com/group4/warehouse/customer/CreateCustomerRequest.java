package com.group4.warehouse.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/customers.
 * Every column of CUSTOMER is NOT NULL in warehouse.sql, so every field is required,
 * and the sizes are the column sizes: the user gets a field error before any SQL runs.
 * The phone is UNIQUE (UQ_CUSTOMER_PHONE); a duplicate is reported by DatabaseErrors.
 */
public record CreateCustomerRequest(

        @NotBlank(message = "Customer ID is required.")
        @Size(max = 10, message = "Customer ID must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Customer ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Customer name is required.")
        @Size(max = 100, message = "Customer name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "\\s*[0-9+() -]{6,20}\\s*",
                message = "Phone must be 6-20 characters: digits, spaces, '+', '-', '(' and ')'.")
        String phone,

        @NotBlank(message = "Address is required.")
        @Size(max = 100, message = "Address must be at most 100 characters.")
        String address) {
}
