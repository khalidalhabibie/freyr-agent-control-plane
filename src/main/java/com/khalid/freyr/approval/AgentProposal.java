package com.khalid.freyr.approval;

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
@Table(name = "agent_proposals")
public class AgentProposal {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "proposal_type", nullable = false)
    private String proposalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentProposalStatus status;

    @Column(nullable = false)
    private String district;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentProposal() {
    }

    public AgentProposal(
            UUID executionId,
            String proposalType,
            AgentProposalStatus status,
            String district,
            LocalDate scheduleDate,
            String summary
    ) {
        this.executionId = executionId;
        this.proposalType = proposalType;
        this.status = status;
        this.district = district;
        this.scheduleDate = scheduleDate;
        this.summary = summary;
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

    public void markApproved() {
        this.status = AgentProposalStatus.APPROVED;
    }

    public void markRejected() {
        this.status = AgentProposalStatus.REJECTED;
    }

    public void markOverridden() {
        this.status = AgentProposalStatus.OVERRIDDEN;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getProposalType() {
        return proposalType;
    }

    public AgentProposalStatus getStatus() {
        return status;
    }

    public String getDistrict() {
        return district;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
