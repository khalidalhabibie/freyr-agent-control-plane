package com.khalid.freyr.approval;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AgentProposalResponse(
        UUID id,
        UUID executionId,
        String proposalType,
        AgentProposalStatus status,
        String district,
        LocalDate scheduleDate,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        List<AgentProposalItemResponse> items
) {
}
