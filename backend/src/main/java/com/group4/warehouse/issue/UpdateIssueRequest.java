package com.group4.warehouse.issue;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/issues/{id}. A missing field (null) is left unchanged.
 * While the issue is Pending everything can change. Moving it to Shipped or Completed
 * takes its lines out of stock; after that only the status can move on (Shipped -> Completed).
 */
public record UpdateIssueRequest(

        String date,

        @Size(max = 10, message = "Order ID must be at most 10 characters.")
        String orderId,

        @Size(max = 50, message = "State of goods must be at most 50 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "State of goods must not be blank.")
        String stateOfGood,

        @Pattern(regexp = IssueStatus.PATTERN, message = IssueStatus.MESSAGE)
        String status,

        List<@Valid DocumentLineRequest> lines) {
}
