package com.khalid.freyr.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentProposalItemResponse(
        UUID id,
        UUID proposalId,
        UUID fieldTaskId,
        UUID recommendedAgronomistId,
        boolean overridden,
        UUID originalAgronomistId,
        UUID overriddenAgronomistId,
        String overrideReason,
        Instant overriddenAt,
        String recommendationReason,
        BigDecimal confidenceScore,
        AgentProposalItemStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
