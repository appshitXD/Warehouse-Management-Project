package com.group4.warehouse.zone;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateZoneRequest(
        @Size(max = 120, message = "Zone name must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Zone name must not be blank.")
        String name) {
}
