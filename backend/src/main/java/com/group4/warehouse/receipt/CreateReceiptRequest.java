package com.group4.warehouse.receipt;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/receipts:
 * { "supplierId": "S10", "lines": [ { "productId": "P12", "binId": "W01|Z01|A02|B009", "quantity": 55 } ] }
 * id (e.g. "GR16") and date ("2026-10-18") are optional: the next GRxx and today are used.
 */
public record CreateReceiptRequest(

        @Size(max = 10, message = "Receipt ID must be at most 10 characters.")
        @Pattern(regexp = "|\\s*[A-Za-z0-9_-]+\\s*",
                message = "Receipt ID may contain only letters, digits, '-' and '_'.")
        String id,

        String date,

        @NotBlank(message = "Supplier is required.")
        @Size(max = 10, message = "Supplier ID must be at most 10 characters.")
        String supplierId,

        @NotEmpty(message = "Add at least one line.")
        List<@Valid DocumentLineRequest> lines) {
}
