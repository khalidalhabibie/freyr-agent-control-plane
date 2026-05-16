package com.khalid.freyr.approval;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OverrideAgentProposalItemRequest(
        @NotNull(message = "proposalItemId is required")
        UUID proposalItemId,

        @NotNull(message = "newAgronomistId is required")
        UUID newAgronomistId
) {
}
