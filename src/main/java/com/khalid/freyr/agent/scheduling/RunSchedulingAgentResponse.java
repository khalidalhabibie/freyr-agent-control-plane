package com.khalid.freyr.agent.scheduling;

import com.khalid.freyr.approval.AgentProposalStatus;

import java.util.UUID;

public record RunSchedulingAgentResponse(
        UUID executionId,
        UUID proposalId,
        AgentProposalStatus status,
        String message
) {
}
