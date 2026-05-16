package com.khalid.freyr.approval;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgentProposalSummaryResponse(
        UUID id,
        UUID executionId,
        String proposalType,
        AgentProposalStatus status,
        String district,
        LocalDate scheduleDate,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {
}
