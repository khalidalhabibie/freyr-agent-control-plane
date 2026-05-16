package com.khalid.freyr.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_proposal_items")
public class AgentProposalItem {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "field_task_id", nullable = false)
    private UUID fieldTaskId;

    @Column(name = "recommended_agronomist_id", nullable = false)
    private UUID recommendedAgronomistId;

    @Column(name = "original_agronomist_id")
    private UUID originalAgronomistId;

    @Column(name = "overridden_agronomist_id")
    private UUID overriddenAgronomistId;

    @Column(name = "override_reason", length = 1000)
    private String overrideReason;

    @Column(name = "overridden_at")
    private Instant overriddenAt;

    @Column(name = "recommendation_reason", nullable = false, length = 1000)
    private String recommendationReason;

    @Column(name = "confidence_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentProposalItemStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentProposalItem() {
    }

    public AgentProposalItem(
            UUID proposalId,
            UUID fieldTaskId,
            UUID recommendedAgronomistId,
            String recommendationReason,
            BigDecimal confidenceScore,
            AgentProposalItemStatus status
    ) {
        this.proposalId = proposalId;
        this.fieldTaskId = fieldTaskId;
        this.recommendedAgronomistId = recommendedAgronomistId;
        this.recommendationReason = recommendationReason;
        this.confidenceScore = confidenceScore;
        this.status = status;
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

    public void markAssigned() {
        this.status = AgentProposalItemStatus.ASSIGNED;
    }

    public void markRejected() {
        this.status = AgentProposalItemStatus.REJECTED;
    }

    public void overrideAgronomist(UUID newAgronomistId, String reason) {
        this.originalAgronomistId = recommendedAgronomistId;
        this.overriddenAgronomistId = newAgronomistId;
        this.overrideReason = reason;
        this.overriddenAt = Instant.now();
        this.recommendedAgronomistId = newAgronomistId;
        this.status = AgentProposalItemStatus.OVERRIDDEN;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public UUID getFieldTaskId() {
        return fieldTaskId;
    }

    public UUID getRecommendedAgronomistId() {
        return recommendedAgronomistId;
    }

    public UUID getOriginalAgronomistId() {
        return originalAgronomistId;
    }

    public UUID getOverriddenAgronomistId() {
        return overriddenAgronomistId;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public Instant getOverriddenAt() {
        return overriddenAt;
    }

    public boolean isOverridden() {
        return overriddenAgronomistId != null;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public AgentProposalItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
