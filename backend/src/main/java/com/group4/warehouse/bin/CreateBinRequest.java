package com.group4.warehouse.bin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBinRequest(
        @NotBlank(message = "Bin position is required.")
        @Size(max = 10, message = "Bin position must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Bin position may contain only letters, digits, '-' and '_'.")
        String position,
        // BIN.Name is NOT NULL VARCHAR(50) in warehouse.sql.
        @NotBlank(message = "Bin name is required.")
        @Size(max = 50, message = "Bin name must be at most 50 characters.")
        String name) {
}
