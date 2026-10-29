package com.group4.warehouse.aisle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAisleRequest(
        @NotBlank(message = "Aisle ID is required.")
        @Size(max = 40, message = "Aisle ID must be at most 40 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Aisle ID may contain only letters, digits, '-' and '_'.")
        String id,
        @NotBlank(message = "Aisle name is required.")
        @Size(max = 120, message = "Aisle name must be at most 120 characters.")
        String name) {
}
