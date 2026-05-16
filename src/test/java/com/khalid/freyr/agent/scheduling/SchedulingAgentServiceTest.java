package com.khalid.freyr.agent.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khalid.freyr.agent.execution.AgentExecutionResponse;
import com.khalid.freyr.agent.execution.AgentExecutionService;
import com.khalid.freyr.agent.execution.AgentExecutionStatus;
import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.approval.AgentProposal;
import com.khalid.freyr.approval.AgentProposalItem;
import com.khalid.freyr.approval.AgentProposalItemRepository;
import com.khalid.freyr.approval.AgentProposalItemStatus;
import com.khalid.freyr.approval.AgentProposalRepository;
import com.khalid.freyr.approval.AgentProposalStatus;
import com.khalid.freyr.assignment.TaskAssignmentRepository;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.FieldTaskRepository;
import com.khalid.freyr.fieldtask.TaskPriority;
import com.khalid.freyr.fieldtask.TaskStatus;
import com.khalid.freyr.fieldtask.TaskType;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingAgentServiceTest {

    @Mock
    private FieldTaskRepository fieldTaskRepository;

    @Mock
    private AgronomistRepository agronomistRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private AgentProposalRepository agentProposalRepository;

    @Mock
    private AgentProposalItemRepository agentProposalItemRepository;

    @Mock
    private AgentExecutionService agentExecutionService;

    private SchedulingAgentService schedulingAgentService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SchedulingProposalWriter proposalWriter = new SchedulingProposalWriter(
                fieldTaskRepository,
                agronomistRepository,
                taskAssignmentRepository,
                agentProposalRepository,
                agentProposalItemRepository,
                agentExecutionService,
                new RuleBasedSchedulingAgent(),
                objectMapper
        );
        schedulingAgentService = new SchedulingAgentService(agentExecutionService, proposalWriter, objectMapper);
    }

    @Test
    void successfulSchedulingCreatesExecutionProposalItemsAndMarksTasksProposed() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        UUID executionId = UUID.randomUUID();
        FieldTask task = fieldTask(TaskPriority.HIGH, TaskStatus.CREATED, scheduleDate, null);
        Agronomist agronomist = agronomist("Sari", 2);

        stubRunningExecution(executionId);
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(task));
        stubAvailableAgronomists("Aceh Besar", List.of(agronomist));
        stubActiveAssignments(agronomist, scheduleDate, 0L);
        stubProposalSave();

        RunSchedulingAgentResponse response = schedulingAgentService.runSchedulingAgent(
                new RunSchedulingAgentRequest("Aceh Besar", scheduleDate)
        );

        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.proposalId()).isNotNull();
        assertThat(response.status()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(response.message()).isEqualTo("Scheduling proposal has been generated and requires approval");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROPOSED);

        ArgumentCaptor<AgentProposalItem> itemCaptor = ArgumentCaptor.forClass(AgentProposalItem.class);
        verify(agentProposalItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFieldTaskId()).isEqualTo(task.getId());
        assertThat(itemCaptor.getValue().getRecommendedAgronomistId()).isEqualTo(agronomist.getId());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        assertThat(itemCaptor.getValue().getConfidenceScore()).isBetween(
                new java.math.BigDecimal("0.0000"),
                new java.math.BigDecimal("1.0000")
        );
        verify(fieldTaskRepository).save(task);
        verify(agentExecutionService).markExecutionSuccess(eq(executionId), any(String.class));
    }

    @Test
    void noEligibleTasksRecordsFailedExecution() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        UUID executionId = UUID.randomUUID();

        stubRunningExecution(executionId);
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of());

        assertThatThrownBy(() -> schedulingAgentService.runSchedulingAgent(
                new RunSchedulingAgentRequest("Aceh Besar", scheduleDate)
        ))
                .isInstanceOf(SchedulingAgentException.class)
                .hasMessage("No eligible field tasks found for scheduling");

        verify(agentExecutionService).markExecutionFailed(
                executionId,
                "No eligible field tasks found for scheduling"
        );
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
    }

    @Test
    void noAvailableAgronomistsRecordsFailedExecution() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        UUID executionId = UUID.randomUUID();
        FieldTask task = fieldTask(TaskPriority.HIGH, TaskStatus.CREATED, scheduleDate, null);

        stubRunningExecution(executionId);
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(task));
        stubAvailableAgronomists("Aceh Besar", List.of());

        assertThatThrownBy(() -> schedulingAgentService.runSchedulingAgent(
                new RunSchedulingAgentRequest("Aceh Besar", scheduleDate)
        ))
                .isInstanceOf(SchedulingAgentException.class)
                .hasMessage("No available agronomists found for scheduling");

        verify(agentExecutionService).markExecutionFailed(
                executionId,
                "No available agronomists found for scheduling"
        );
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CREATED);
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
    }

    @Test
    void capacityLimitIsRespected() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FieldTask highPriorityTask = fieldTask(TaskPriority.HIGH, TaskStatus.CREATED, scheduleDate, null);
        FieldTask lowPriorityTask = fieldTask(TaskPriority.LOW, TaskStatus.CREATED, scheduleDate, null);
        Agronomist agronomist = agronomist("Sari", 1);

        stubRunningExecution(UUID.randomUUID());
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(highPriorityTask, lowPriorityTask));
        stubAvailableAgronomists("Aceh Besar", List.of(agronomist));
        stubActiveAssignments(agronomist, scheduleDate, 0L);
        stubProposalSave();

        schedulingAgentService.runSchedulingAgent(new RunSchedulingAgentRequest("Aceh Besar", scheduleDate));

        ArgumentCaptor<AgentProposalItem> itemCaptor = ArgumentCaptor.forClass(AgentProposalItem.class);
        verify(agentProposalItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFieldTaskId()).isEqualTo(highPriorityTask.getId());
        assertThat(highPriorityTask.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(lowPriorityTask.getStatus()).isEqualTo(TaskStatus.CREATED);
    }

    @Test
    void overdueTodayAndTomorrowTasksAreIncluded() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FieldTask overdue = fieldTask(TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate.minusDays(1), null);
        FieldTask today = fieldTask(TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate, null);
        FieldTask tomorrow = fieldTask(TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate.plusDays(1), null);
        Agronomist agronomist = agronomist("Sari", 3);

        stubRunningExecution(UUID.randomUUID());
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(overdue, today, tomorrow));
        stubAvailableAgronomists("Aceh Besar", List.of(agronomist));
        stubActiveAssignments(agronomist, scheduleDate, 0L);
        stubProposalSave();

        schedulingAgentService.runSchedulingAgent(new RunSchedulingAgentRequest("Aceh Besar", scheduleDate));

        assertThat(overdue.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(today.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(tomorrow.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        ArgumentCaptor<AgentProposalItem> itemCaptor = ArgumentCaptor.forClass(AgentProposalItem.class);
        verify(agentProposalItemRepository, org.mockito.Mockito.times(3)).save(itemCaptor.capture());
    }

    @Test
    void tasksAfterTomorrowAreExcluded() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FieldTask today = fieldTask(TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate, null);
        FieldTask afterTomorrow = fieldTask(TaskPriority.CRITICAL, TaskStatus.CREATED, scheduleDate.plusDays(2), null);
        Agronomist agronomist = agronomist("Sari", 2);

        stubRunningExecution(UUID.randomUUID());
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(today));
        stubAvailableAgronomists("Aceh Besar", List.of(agronomist));
        stubActiveAssignments(agronomist, scheduleDate, 0L);
        stubProposalSave();

        schedulingAgentService.runSchedulingAgent(new RunSchedulingAgentRequest("Aceh Besar", scheduleDate));

        assertThat(today.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(afterTomorrow.getStatus()).isEqualTo(TaskStatus.CREATED);
    }

    @Test
    void nonCreatedTasksAreExcluded() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FieldTask created = fieldTask(TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate, null);
        FieldTask alreadyProposed = fieldTask(TaskPriority.CRITICAL, TaskStatus.PROPOSED, scheduleDate, null);
        Agronomist agronomist = agronomist("Sari", 2);

        stubRunningExecution(UUID.randomUUID());
        stubEligibleTasks("Aceh Besar", scheduleDate, List.of(created));
        stubAvailableAgronomists("Aceh Besar", List.of(agronomist));
        stubActiveAssignments(agronomist, scheduleDate, 0L);
        stubProposalSave();

        schedulingAgentService.runSchedulingAgent(new RunSchedulingAgentRequest("Aceh Besar", scheduleDate));

        assertThat(created.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        assertThat(alreadyProposed.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        verify(fieldTaskRepository, never()).save(alreadyProposed);
    }

    private void stubRunningExecution(UUID executionId) {
        when(agentExecutionService.createRunningExecution(
                eq("rule-based-scheduling-agent"),
                eq("SCHEDULING"),
                any(String.class),
                eq("rules-v1"),
                eq("rule-based-v1")
        )).thenReturn(new AgentExecutionResponse(
                executionId,
                "rule-based-scheduling-agent",
                "SCHEDULING",
                "{\"district\":\"Aceh Besar\"}",
                null,
                AgentExecutionStatus.RUNNING,
                "rules-v1",
                "rule-based-v1",
                null,
                Instant.now(),
                null,
                Instant.now()
        ));
    }

    private void stubEligibleTasks(String district, LocalDate scheduleDate, List<FieldTask> tasks) {
        when(fieldTaskRepository.findEligibleUnassignedTasksByDistrictAndDueDateWindow(
                district,
                scheduleDate.plusDays(1)
        )).thenReturn(tasks);
    }

    private void stubAvailableAgronomists(String district, List<Agronomist> agronomists) {
        when(agronomistRepository.findByAssignedDistrictAndAvailabilityStatus(
                district,
                AvailabilityStatus.AVAILABLE
        )).thenReturn(agronomists);
    }

    private void stubActiveAssignments(Agronomist agronomist, LocalDate scheduleDate, long count) {
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                agronomist.getId(),
                scheduleDate
        )).thenReturn(count);
    }

    private void stubProposalSave() {
        when(agentProposalRepository.save(any(AgentProposal.class))).thenAnswer(invocation -> {
            AgentProposal proposal = invocation.getArgument(0);
            ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(proposal, "createdAt", Instant.now());
            ReflectionTestUtils.setField(proposal, "updatedAt", Instant.now());
            return proposal;
        });
    }

    private FieldTask fieldTask(TaskPriority priority, TaskStatus status, LocalDate dueDate, UUID assignedAgronomistId) {
        FieldTask fieldTask = new FieldTask(
                UUID.randomUUID(),
                TaskType.WATER_LEVEL_CHECK,
                priority,
                status,
                dueDate,
                assignedAgronomistId,
                null
        );
        ReflectionTestUtils.setField(fieldTask, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(fieldTask, "createdAt", Instant.now());
        ReflectionTestUtils.setField(fieldTask, "updatedAt", Instant.now());
        return fieldTask;
    }

    private Agronomist agronomist(String name, int maxDailyVisit) {
        Agronomist agronomist = new Agronomist(
                name,
                "08123456789",
                "Aceh Besar",
                maxDailyVisit,
                AvailabilityStatus.AVAILABLE
        );
        ReflectionTestUtils.setField(agronomist, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(agronomist, "createdAt", Instant.now());
        ReflectionTestUtils.setField(agronomist, "updatedAt", Instant.now());
        return agronomist;
    }
}
