package com.khalid.freyr.approval;

import jakarta.validation.constraints.NotBlank;

public record RejectAgentProposalRequest(
        @NotBlank(message = "reviewedBy is required")
        String reviewedBy,

        @NotBlank(message = "reason is required")
        String reason
) {
}
