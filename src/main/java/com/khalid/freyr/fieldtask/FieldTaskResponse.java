package com.khalid.freyr.fieldtask;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FieldTaskResponse(
        UUID id,
        UUID farmFieldId,
        TaskType taskType,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        UUID assignedAgronomistId,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
