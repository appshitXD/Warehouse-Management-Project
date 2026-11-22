package com.group4.warehouse.aisle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAisleRequest(
        @NotBlank(message = "Aisle ID is required.")
        @Size(max = 10, message = "Aisle ID must be at most 10 characters.")
        @Pattern(regexp = "\\s*[A-Za-z0-9_-]+\\s*",
                message = "Aisle ID may contain only letters, digits, '-' and '_'.")
        String id,
        @NotBlank(message = "Aisle name is required.")
        @Size(max = 100, message = "Aisle name must be at most 100 characters.")
        String name) {
}
