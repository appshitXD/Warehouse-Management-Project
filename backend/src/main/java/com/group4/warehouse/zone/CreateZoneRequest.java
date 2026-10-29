package com.group4.warehouse.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateZoneRequest(
        @NotBlank(message = "Zone ID is required.")
        @Size(max = 40, message = "Zone ID must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Zone ID may contain only letters, digits, '-' and '_'.")
        String id,
        @NotBlank(message = "Zone name is required.")
        @Size(max = 120, message = "Zone name must be at most 120 characters.")
        String name) {
}
