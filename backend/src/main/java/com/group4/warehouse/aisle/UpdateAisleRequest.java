package com.group4.warehouse.aisle;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAisleRequest(
        @Size(max = 100, message = "Aisle name must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Aisle name must not be blank.")
        String name) {
}
