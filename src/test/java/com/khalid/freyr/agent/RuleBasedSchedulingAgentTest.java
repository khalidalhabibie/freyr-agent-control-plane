package com.khalid.freyr.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.approval.AgentProposal;
import com.khalid.freyr.approval.AgentProposalItem;
import com.khalid.freyr.approval.AgentProposalItemRepository;
import com.khalid.freyr.approval.AgentProposalRepository;
import com.khalid.freyr.approval.AgentProposalStatus;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.FieldTaskRepository;
import com.khalid.freyr.fieldtask.TaskPriority;
import com.khalid.freyr.fieldtask.TaskStatus;
import com.khalid.freyr.fieldtask.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleBasedSchedulingAgentTest {

    @Mock
    private AgentExecutionRepository agentExecutionRepository;

    @Mock
    private FieldTaskRepository fieldTaskRepository;

    @Mock
    private AgronomistRepository agronomistRepository;

    @Mock
    private AgentProposalRepository agentProposalRepository;

    @Mock
    private AgentProposalItemRepository agentProposalItemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void runCreatesProposalItemsByPriorityAndCapacity() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FieldTask lowTask = fieldTask(TaskPriority.LOW, scheduleDate);
        FieldTask criticalTask = fieldTask(TaskPriority.CRITICAL, scheduleDate);
        Agronomist agronomist = agronomist("Dewi Lestari", "Aceh Besar", 1);
        UUID proposalId = UUID.randomUUID();

        when(agentExecutionRepository.save(any(AgentExecution.class))).thenAnswer(invocation -> {
            AgentExecution execution = invocation.getArgument(0);
            if (execution.getId() == null) {
                execution.prePersist();
            }
            return execution;
        });
        when(fieldTaskRepository.findUnassignedTasksByDistrictAndDueDate("Aceh Besar", scheduleDate))
                .thenReturn(List.of(lowTask, criticalTask));
        when(agronomistRepository.findByAssignedDistrictAndAvailabilityStatus("Aceh Besar", AvailabilityStatus.AVAILABLE))
                .thenReturn(List.of(agronomist));
        when(agentProposalRepository.save(any(AgentProposal.class))).thenAnswer(invocation -> {
            AgentProposal proposal = invocation.getArgument(0);
            setPersistedTimestamps(proposal, proposalId);
            return proposal;
        });

        RunSchedulingAgentResponse response = schedulingAgent().run(new RunSchedulingAgentRequest(
                "Aceh Besar",
                scheduleDate
        ));

        assertThat(response.executionId()).isNotNull();
        assertThat(response.proposalId()).isEqualTo(proposalId);
        assertThat(response.status()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL.name());
        assertThat(response.message()).isEqualTo("Scheduling proposal has been generated and requires approval");
        assertThat(criticalTask.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(lowTask.getStatus()).isEqualTo(TaskStatus.CREATED);

        ArgumentCaptor<AgentProposalItem> itemCaptor = ArgumentCaptor.forClass(AgentProposalItem.class);
        verify(agentProposalItemRepository).save(itemCaptor.capture());
        AgentProposalItem item = itemCaptor.getValue();
        assertThat(item.getProposalId()).isEqualTo(proposalId);
        assertThat(item.getFieldTaskId()).isEqualTo(criticalTask.getId());
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(agronomist.getId());
        assertThat(item.getConfidenceScore()).isEqualByComparingTo("1.0000");

        ArgumentCaptor<AgentExecution> executionCaptor = ArgumentCaptor.forClass(AgentExecution.class);
        verify(agentExecutionRepository, org.mockito.Mockito.times(2)).save(executionCaptor.capture());
        AgentExecution completedExecution = executionCaptor.getAllValues().getLast();
        assertThat(completedExecution.getStatus()).isEqualTo(AgentExecutionStatus.SUCCESS);
        assertThat(completedExecution.getOutputPayload()).contains(proposalId.toString());
    }

    @Test
    void runFailsExecutionWhenNoAgronomistIsAvailable() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);

        when(agentExecutionRepository.save(any(AgentExecution.class))).thenAnswer(invocation -> {
            AgentExecution execution = invocation.getArgument(0);
            if (execution.getId() == null) {
                execution.prePersist();
            }
            return execution;
        });
        when(fieldTaskRepository.findUnassignedTasksByDistrictAndDueDate("Aceh Besar", scheduleDate))
                .thenReturn(List.of(fieldTask(TaskPriority.CRITICAL, scheduleDate)));
        when(agronomistRepository.findByAssignedDistrictAndAvailabilityStatus("Aceh Besar", AvailabilityStatus.AVAILABLE))
                .thenReturn(List.of());

        RunSchedulingAgentResponse response = schedulingAgent().run(new RunSchedulingAgentRequest(
                "Aceh Besar",
                scheduleDate
        ));

        assertThat(response.executionId()).isNotNull();
        assertThat(response.proposalId()).isNull();
        assertThat(response.status()).isEqualTo(AgentExecutionStatus.FAILED.name());
        assertThat(response.message()).isEqualTo("No available agronomist found for district Aceh Besar");
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));

        ArgumentCaptor<AgentExecution> executionCaptor = ArgumentCaptor.forClass(AgentExecution.class);
        verify(agentExecutionRepository, org.mockito.Mockito.times(2)).save(executionCaptor.capture());
        AgentExecution failedExecution = executionCaptor.getAllValues().getLast();
        assertThat(failedExecution.getStatus()).isEqualTo(AgentExecutionStatus.FAILED);
        assertThat(failedExecution.getErrorMessage()).isEqualTo("No available agronomist found for district Aceh Besar");
    }

    private FieldTask fieldTask(TaskPriority priority, LocalDate dueDate) {
        FieldTask fieldTask = new FieldTask(
                UUID.randomUUID(),
                TaskType.WATER_LEVEL_CHECK,
                priority,
                TaskStatus.CREATED,
                dueDate,
                null,
                null
        );
        ReflectionTestUtils.setField(fieldTask, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(fieldTask, "createdAt", Instant.now());
        ReflectionTestUtils.setField(fieldTask, "updatedAt", Instant.now());
        return fieldTask;
    }

    private Agronomist agronomist(String name, String district, int maxDailyVisit) {
        Agronomist agronomist = new Agronomist(
                name,
                "08123456789",
                district,
                maxDailyVisit,
                AvailabilityStatus.AVAILABLE
        );
        ReflectionTestUtils.setField(agronomist, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(agronomist, "createdAt", Instant.now());
        ReflectionTestUtils.setField(agronomist, "updatedAt", Instant.now());
        return agronomist;
    }

    private void setPersistedTimestamps(AgentProposal proposal, UUID id) {
        ReflectionTestUtils.setField(proposal, "id", id);
        ReflectionTestUtils.setField(proposal, "createdAt", Instant.now());
        ReflectionTestUtils.setField(proposal, "updatedAt", Instant.now());
    }

    private RuleBasedSchedulingAgent schedulingAgent() {
        return new RuleBasedSchedulingAgent(
                agentExecutionRepository,
                fieldTaskRepository,
                agronomistRepository,
                agentProposalRepository,
                agentProposalItemRepository,
                objectMapper
        );
    }
}
