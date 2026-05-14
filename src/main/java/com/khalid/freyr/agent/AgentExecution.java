package com.khalid.freyr.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_executions")
public class AgentExecution {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Column(name = "execution_type", nullable = false)
    private String executionType;

    @Column(name = "input_payload", nullable = false, columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentExecutionStatus status;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentExecution() {
    }

    public AgentExecution(
            String agentName,
            String executionType,
            String inputPayload,
            String outputPayload,
            AgentExecutionStatus status,
            String modelName,
            String promptVersion,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {
        this.agentName = agentName;
        this.executionType = executionType;
        this.inputPayload = inputPayload;
        this.outputPayload = outputPayload;
        this.status = status;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markSuccess(String outputPayload) {
        this.status = AgentExecutionStatus.SUCCESS;
        this.outputPayload = outputPayload;
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = AgentExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getExecutionType() {
        return executionType;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public AgentExecutionStatus getStatus() {
        return status;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
