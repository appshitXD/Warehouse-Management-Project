package com.group4.warehouse.task;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of PATCH /api/tasks/{id}. A missing field (null) is left unchanged. */
public record UpdateTaskRequest(

        @Size(max = 100, message = "Task name must be at most 100 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Task name must not be blank.")
        String name,

        @Size(max = 10, message = "Staff ID must be at most 10 characters.")
        String staffId,

        @Pattern(regexp = Task.STATUS_PATTERN, message = Task.STATUS_MESSAGE)
        String status) {
}
