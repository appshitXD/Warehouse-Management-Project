package com.group4.warehouse.bin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBinRequest(
        @NotBlank(message = "Bin position is required.")
        @Size(max = 40, message = "Bin position must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Bin position may contain only letters, digits, '-' and '_'.")
        String position,
        @Size(max = 120, message = "Bin name must be at most 120 characters.")
        String name) {
}
