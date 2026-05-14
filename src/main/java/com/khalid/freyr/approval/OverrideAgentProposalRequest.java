package com.khalid.freyr.approval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OverrideAgentProposalRequest(
        @NotBlank(message = "reviewedBy is required")
        String reviewedBy,

        @NotBlank(message = "reason is required")
        String reason,

        @NotEmpty(message = "overrides is required")
        List<@Valid OverrideAgentProposalItemRequest> overrides
) {
}
