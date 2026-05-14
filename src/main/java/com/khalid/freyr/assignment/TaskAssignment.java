package com.khalid.freyr.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "field_task_id", nullable = false)
    private UUID fieldTaskId;

    @Column(name = "agronomist_id", nullable = false)
    private UUID agronomistId;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskAssignmentStatus status;

    protected TaskAssignment() {
    }

    public TaskAssignment(UUID fieldTaskId, UUID agronomistId, UUID proposalId) {
        this.fieldTaskId = fieldTaskId;
        this.agronomistId = agronomistId;
        this.proposalId = proposalId;
        this.status = TaskAssignmentStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFieldTaskId() {
        return fieldTaskId;
    }

    public UUID getAgronomistId() {
        return agronomistId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public TaskAssignmentStatus getStatus() {
        return status;
    }
}
