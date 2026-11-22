package com.group4.warehouse.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateZoneRequest(
        @NotBlank(message = "Zone ID is required.")
        @Size(max = 10, message = "Zone ID must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Zone ID may contain only letters, digits, '-' and '_'.")
        String id,
        @NotBlank(message = "Zone name is required.")
        @Size(max = 100, message = "Zone name must be at most 100 characters.")
        String name) {
}
