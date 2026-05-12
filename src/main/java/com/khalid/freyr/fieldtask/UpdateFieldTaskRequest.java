package com.khalid.freyr.fieldtask;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateFieldTaskRequest(
        @NotNull(message = "farmFieldId is required")
        UUID farmFieldId,

        @NotNull(message = "taskType is required")
        TaskType taskType,

        @NotNull(message = "priority is required")
        TaskPriority priority,

        @NotNull(message = "status is required")
        TaskStatus status,

        @NotNull(message = "dueDate is required")
        LocalDate dueDate,

        UUID assignedAgronomistId,

        Instant completedAt
) {
}
