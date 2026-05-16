package com.khalid.freyr.agent.execution;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentExecutionService {

    private final AgentExecutionRepository agentExecutionRepository;

    public AgentExecutionService(AgentExecutionRepository agentExecutionRepository) {
        this.agentExecutionRepository = agentExecutionRepository;
    }

    @Transactional
    public AgentExecutionResponse createRunningExecution(
            String agentName,
            String executionType,
            String inputPayload,
            String modelName,
            String promptVersion
    ) {
        AgentExecution execution = new AgentExecution(
                agentName,
                executionType,
                inputPayload,
                modelName,
                promptVersion
        );

        AgentExecution savedExecution = agentExecutionRepository.save(execution);
        return toResponse(savedExecution);
    }

    @Transactional
    public AgentExecutionResponse markExecutionSuccess(UUID id, String outputPayload) {
        AgentExecution execution = getExecutionEntity(id);
        execution.markSuccess(outputPayload);

        AgentExecution savedExecution = agentExecutionRepository.save(execution);
        return toResponse(savedExecution);
    }

    @Transactional
    public AgentExecutionResponse markExecutionFailed(UUID id, String errorMessage) {
        AgentExecution execution = getExecutionEntity(id);
        execution.markFailed(errorMessage);

        AgentExecution savedExecution = agentExecutionRepository.save(execution);
        return toResponse(savedExecution);
    }

    @Transactional(readOnly = true)
    public AgentExecutionResponse getExecution(UUID id) {
        return toResponse(getExecutionEntity(id));
    }

    @Transactional(readOnly = true)
    public List<AgentExecutionResponse> getExecutions() {
        return agentExecutionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AgentExecution getExecutionEntity(UUID id) {
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
