package com.group4.warehouse.receipt;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/receipts/{id}. A missing field (null) is left unchanged.
 * Sending lines replaces all the lines; the stock moves by the difference.
 */
public record UpdateReceiptRequest(

        String date,

        @Size(max = 10, message = "Supplier ID must be at most 10 characters.")
        String supplierId,

        List<@Valid DocumentLineRequest> lines) {
}
