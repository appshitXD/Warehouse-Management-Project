package com.group4.warehouse.customer;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/customers/{id}.
 * A missing field (null) is left unchanged; "" clears phone or address.
 */
public record UpdateCustomerRequest(

        @Size(max = 120, message = "Customer name must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Customer name must not be blank.")
        String name,

        @Pattern(regexp = "|\\s*[0-9+() -]{6,20}\\s*",
                message = "Phone must be 6-20 characters: digits, spaces, '+', '-', '(' and ')'.")
        String phone,

        @Size(max = 250, message = "Address must be at most 250 characters.")
        String address) {
}
