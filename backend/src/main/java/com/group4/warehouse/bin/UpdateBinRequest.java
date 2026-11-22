package com.group4.warehouse.bin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBinRequest(
        // BIN.Name is NOT NULL, so a name that is sent must not be blank.
        @Size(max = 50, message = "Bin name must be at most 50 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Bin name must not be blank.")
        String name) {
}
