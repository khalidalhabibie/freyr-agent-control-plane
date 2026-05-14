package com.khalid.freyr.agent;

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

    @Transactional(readOnly = true)
    public List<AgentExecutionResponse> getAgentExecutions() {
        return agentExecutionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentExecutionResponse getAgentExecution(UUID id) {
        return agentExecutionRepository.findById(id)
                .map(this::toResponse)
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
