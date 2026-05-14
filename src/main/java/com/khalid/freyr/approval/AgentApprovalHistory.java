package com.khalid.freyr.approval;

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
@Table(name = "agent_approval_history")
public class AgentApprovalHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentApprovalAction action;

    @Column(name = "reviewed_by", nullable = false)
    private String reviewedBy;

    @Column(length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentApprovalHistory() {
    }

    public AgentApprovalHistory(UUID proposalId, AgentApprovalAction action, String reviewedBy, String reason) {
        this.proposalId = proposalId;
        this.action = action;
        this.reviewedBy = reviewedBy;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public AgentApprovalAction getAction() {
        return action;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
