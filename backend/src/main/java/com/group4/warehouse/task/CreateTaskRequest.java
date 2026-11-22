package com.group4.warehouse.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/tasks: { "name": "Morning Putaway", "staffId": "ST02" }
 * id and status are optional: next Txx, and the column DEFAULT 'Pending'.
 */
public record CreateTaskRequest(

        @Size(max = 10, message = "Task ID must be at most 10 characters.")
        @Pattern(regexp = "|\\s*[A-Za-z0-9_-]+\\s*",
                message = "Task ID may contain only letters, digits, '-' and '_'.")
        String id,

        @NotBlank(message = "Task name is required.")
        @Size(max = 100, message = "Task name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Staff member is required.")
        @Size(max = 10, message = "Staff ID must be at most 10 characters.")
        String staffId,

        @Pattern(regexp = Task.STATUS_PATTERN, message = Task.STATUS_MESSAGE)
        String status) {
}
