package com.khalid.freyr.fieldtask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "field_tasks")
public class FieldTask {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "farm_field_id", nullable = false)
    private UUID farmFieldId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 80)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskStatus status;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "assigned_agronomist_id")
    private UUID assignedAgronomistId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FieldTask() {
    }

    public FieldTask(
            UUID farmFieldId,
            TaskType taskType,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            UUID assignedAgronomistId,
            Instant completedAt
    ) {
        this.farmFieldId = farmFieldId;
        this.taskType = taskType;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.assignedAgronomistId = assignedAgronomistId;
        this.completedAt = completedAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            UUID farmFieldId,
            TaskType taskType,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            UUID assignedAgronomistId,
            Instant completedAt
    ) {
        this.farmFieldId = farmFieldId;
        this.taskType = taskType;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.assignedAgronomistId = assignedAgronomistId;
        this.completedAt = completedAt;
    }

    public void markAssigned(UUID agronomistId) {
        this.assignedAgronomistId = agronomistId;
        this.status = TaskStatus.ASSIGNED;
    }

    public void markCreated() {
        this.assignedAgronomistId = null;
        this.status = TaskStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFarmFieldId() {
        return farmFieldId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public UUID getAssignedAgronomistId() {
        return assignedAgronomistId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
