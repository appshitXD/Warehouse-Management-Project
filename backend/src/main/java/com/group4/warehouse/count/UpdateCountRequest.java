package com.group4.warehouse.count;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * Body of PATCH /api/counts/{id}. A missing field (null) is left unchanged.
 * Allowed until the count is Completed; setting status to Completed applies it to stock.
 */
public record UpdateCountRequest(

        String date,

        @Pattern(regexp = CountStatus.PATTERN, message = CountStatus.MESSAGE)
        String status,

        List<@Valid DocumentLineRequest> lines) {
}
