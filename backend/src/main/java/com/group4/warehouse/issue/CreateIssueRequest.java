package com.group4.warehouse.issue;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/issues:
 * { "orderId": "CO27", "stateOfGood": "Good", "status": "Pending",
 *   "lines": [ { "productId": "P01", "binId": "W02|Z02|A02|B007", "quantity": 5 } ] }
 * id ("GI17"), date and status are optional: next GIxx, today, and the column DEFAULT 'Pending'.
 * Stock is taken out of the bins only when the status is Shipped or Completed.
 */
public record CreateIssueRequest(

        @Size(max = 10, message = "Issue ID must be at most 10 characters.")
        @Pattern(regexp = "|\\s*[A-Za-z0-9_-]+\\s*",
                message = "Issue ID may contain only letters, digits, '-' and '_'.")
        String id,

        String date,

        @NotBlank(message = "Customer order is required.")
        @Size(max = 10, message = "Order ID must be at most 10 characters.")
        String orderId,

        @NotBlank(message = "State of goods is required.")
        @Size(max = 50, message = "State of goods must be at most 50 characters.")
        String stateOfGood,

        @Pattern(regexp = IssueStatus.PATTERN, message = IssueStatus.MESSAGE)
        String status,

        @NotEmpty(message = "Add at least one line.")
        List<@Valid DocumentLineRequest> lines) {
}
