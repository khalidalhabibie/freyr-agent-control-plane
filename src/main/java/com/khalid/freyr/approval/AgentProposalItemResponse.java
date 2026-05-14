package com.khalid.freyr.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentProposalItemResponse(
        UUID id,
        UUID proposalId,
        UUID fieldTaskId,
        UUID recommendedAgronomistId,
        String recommendationReason,
        BigDecimal confidenceScore,
        AgentProposalItemStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
