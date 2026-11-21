package com.group4.warehouse.count;

import java.util.List;

import com.group4.warehouse.common.DocumentLineRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/counts:
 * { "status": "Completed", "lines": [ { "productId": "P01", "binId": "W02|Z02|A02|B007", "quantity": 18 } ] }
 * quantity = the counted quantity (0 allowed). The recorded quantity is read from PRODUCT_BIN.
 * id ("SC20"), date and status are optional: next SCxx, now, and the column DEFAULT 'Pending'.
 */
public record CreateCountRequest(

        @Size(max = 10, message = "Count ID must be at most 10 characters.")
        @Pattern(regexp = "|\\s*[A-Za-z0-9_-]+\\s*",
                message = "Count ID may contain only letters, digits, '-' and '_'.")
        String id,

        String date,

        @Pattern(regexp = CountStatus.PATTERN, message = CountStatus.MESSAGE)
        String status,

        @NotEmpty(message = "Add at least one line.")
        List<@Valid DocumentLineRequest> lines) {
}
