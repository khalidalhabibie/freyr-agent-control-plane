package com.khalid.freyr.approval;

import jakarta.validation.constraints.NotBlank;

public record ApproveAgentProposalRequest(
        @NotBlank(message = "reviewedBy is required")
        String reviewedBy,

        String note
) {
}
