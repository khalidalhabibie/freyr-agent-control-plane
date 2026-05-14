package com.khalid.freyr.agent;

import java.util.UUID;

public record RunSchedulingAgentResponse(
        UUID executionId,
        UUID proposalId,
        String status,
        String message
) {
}
