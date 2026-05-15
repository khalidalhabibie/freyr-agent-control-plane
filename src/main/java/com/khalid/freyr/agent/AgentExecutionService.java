package com.khalid.freyr.agent;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentExecutionService {

    private final AgentExecutionRepository agentExecutionRepository;

    public AgentExecutionService(AgentExecutionRepository agentExecutionRepository) {
        this.agentExecutionRepository = agentExecutionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentExecution createRunningExecution(
            String agentName,
            String executionType,
            String inputPayload,
            String modelName,
            String promptVersion
    ) {
        return agentExecutionRepository.save(new AgentExecution(
                agentName,
                executionType,
                inputPayload,
                null,
                AgentExecutionStatus.RUNNING,
                modelName,
                promptVersion,
                null,
                Instant.now(),
                null
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID id, String outputPayload) {
        AgentExecution execution = getExecution(id);
        execution.markSuccess(outputPayload);
        agentExecutionRepository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, String errorMessage) {
        AgentExecution execution = getExecution(id);
        execution.markFailed(errorMessage);
        agentExecutionRepository.save(execution);
    }

    @Transactional(readOnly = true)
    public List<AgentExecutionResponse> getAgentExecutions() {
        return agentExecutionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentExecutionResponse getAgentExecution(UUID id) {
        return toResponse(getExecution(id));
    }

    private AgentExecution getExecution(UUID id) {
        return agentExecutionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent execution not found"));
    }

    private AgentExecutionResponse toResponse(AgentExecution execution) {
        return new AgentExecutionResponse(
                execution.getId(),
                execution.getAgentName(),
                execution.getExecutionType(),
                execution.getInputPayload(),
                execution.getOutputPayload(),
                execution.getStatus(),
                execution.getModelName(),
                execution.getPromptVersion(),
                execution.getErrorMessage(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedAt()
        );
    }
}
