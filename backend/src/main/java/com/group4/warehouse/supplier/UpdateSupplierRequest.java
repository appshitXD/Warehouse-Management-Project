package com.group4.warehouse.supplier;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/suppliers/{id}.
 * A missing field (null) is left unchanged. A field that is sent must not be blank,
 * because every column of SUPPLIER is NOT NULL.
 */
public record UpdateSupplierRequest(

        @Size(max = 100, message = "Supplier name must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Supplier name must not be blank.")
        String name,

        @Pattern(regexp = "\\s*[0-9+() -]{6,20}\\s*",
                message = "Phone must be 6-20 characters: digits, spaces, '+', '-', '(' and ')'.")
        String phone,

        @Size(max = 100, message = "Address must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Address must not be blank.")
        String address) {
}
