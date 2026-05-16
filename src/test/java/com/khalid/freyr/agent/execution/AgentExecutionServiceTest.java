package com.khalid.freyr.agent.execution;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentExecutionServiceTest {

    @Mock
    private AgentExecutionRepository agentExecutionRepository;

    @InjectMocks
    private AgentExecutionService agentExecutionService;

    @Test
    void createRunningExecutionSavesAndReturnsRunningExecution() {
        when(agentExecutionRepository.save(any(AgentExecution.class))).thenAnswer(invocation -> {
            AgentExecution execution = invocation.getArgument(0);
            execution.prePersist();
            return execution;
        });

        AgentExecutionResponse response = agentExecutionService.createRunningExecution(
                "rule-based-scheduler",
                "FIELD_TASK_ASSIGNMENT",
                "{\"district\":\"Aceh Besar\"}",
                "rules-v1",
                "scheduler-v1"
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.agentName()).isEqualTo("rule-based-scheduler");
        assertThat(response.executionType()).isEqualTo("FIELD_TASK_ASSIGNMENT");
        assertThat(response.inputPayload()).isEqualTo("{\"district\":\"Aceh Besar\"}");
        assertThat(response.outputPayload()).isNull();
        assertThat(response.status()).isEqualTo(AgentExecutionStatus.RUNNING);
        assertThat(response.modelName()).isEqualTo("rules-v1");
        assertThat(response.promptVersion()).isEqualTo("scheduler-v1");
        assertThat(response.errorMessage()).isNull();
        assertThat(response.startedAt()).isNotNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.createdAt()).isNotNull();
        verify(agentExecutionRepository).save(any(AgentExecution.class));
    }

    @Test
    void markExecutionSuccessUpdatesStatusOutputPayloadAndCompletedAt() {
        AgentExecution execution = persistedExecution();
        UUID id = execution.getId();

        when(agentExecutionRepository.findById(id)).thenReturn(Optional.of(execution));
        when(agentExecutionRepository.save(execution)).thenReturn(execution);

        AgentExecutionResponse response = agentExecutionService.markExecutionSuccess(
                id,
                "{\"proposalId\":\"11111111-1111-1111-1111-111111111111\"}"
        );

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(AgentExecutionStatus.SUCCESS);
        assertThat(response.outputPayload())
                .isEqualTo("{\"proposalId\":\"11111111-1111-1111-1111-111111111111\"}");
        assertThat(response.errorMessage()).isNull();
        assertThat(response.completedAt()).isNotNull();
        verify(agentExecutionRepository).save(execution);
    }

    @Test
    void markExecutionFailedUpdatesStatusErrorMessageAndCompletedAt() {
        AgentExecution execution = persistedExecution();
        UUID id = execution.getId();

        when(agentExecutionRepository.findById(id)).thenReturn(Optional.of(execution));
        when(agentExecutionRepository.save(execution)).thenReturn(execution);

        AgentExecutionResponse response = agentExecutionService.markExecutionFailed(
                id,
                "No available agronomists"
        );

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(AgentExecutionStatus.FAILED);
        assertThat(response.errorMessage()).isEqualTo("No available agronomists");
        assertThat(response.completedAt()).isNotNull();
        verify(agentExecutionRepository).save(execution);
    }

    @Test
    void getExecutionReturnsExecutionWhenFound() {
        AgentExecution execution = persistedExecution();
        UUID id = execution.getId();

        when(agentExecutionRepository.findById(id)).thenReturn(Optional.of(execution));

        AgentExecutionResponse response = agentExecutionService.getExecution(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.agentName()).isEqualTo("rule-based-scheduler");
        assertThat(response.status()).isEqualTo(AgentExecutionStatus.RUNNING);
    }

    @Test
    void getExecutionThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(agentExecutionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentExecutionService.getExecution(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Agent execution not found");
    }

    @Test
    void getExecutionsReturnsAllExecutions() {
        AgentExecution execution = persistedExecution();

        when(agentExecutionRepository.findAll()).thenReturn(List.of(execution));

        List<AgentExecutionResponse> responses = agentExecutionService.getExecutions();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(execution.getId());
        assertThat(responses.getFirst().agentName()).isEqualTo("rule-based-scheduler");
    }

    private AgentExecution persistedExecution() {
        AgentExecution execution = new AgentExecution(
                "rule-based-scheduler",
                "FIELD_TASK_ASSIGNMENT",
                "{\"district\":\"Aceh Besar\"}",
                "rules-v1",
                "scheduler-v1"
        );
        execution.prePersist();
        return execution;
    }
}
