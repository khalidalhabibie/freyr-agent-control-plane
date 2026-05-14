package com.khalid.freyr.agent;

import java.time.Instant;
import java.util.UUID;

public record AgentExecutionResponse(
        UUID id,
        String agentName,
        String executionType,
        String inputPayload,
        String outputPayload,
        AgentExecutionStatus status,
        String modelName,
        String promptVersion,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
