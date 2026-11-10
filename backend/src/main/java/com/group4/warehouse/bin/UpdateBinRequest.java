package com.group4.warehouse.bin;

import jakarta.validation.constraints.Size;

public record UpdateBinRequest(
        @Size(max = 120, message = "Bin name must be at most 120 characters.")
        String name) {
}
