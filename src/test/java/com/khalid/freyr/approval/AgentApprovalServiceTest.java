package com.khalid.freyr.approval;

import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.assignment.TaskAssignment;
import com.khalid.freyr.assignment.TaskAssignmentRepository;
import com.khalid.freyr.assignment.TaskAssignmentStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentApprovalServiceTest {

    @Mock
    private AgentProposalRepository agentProposalRepository;

    @Mock
    private AgentProposalItemRepository agentProposalItemRepository;

    @Mock
    private AgentApprovalHistoryRepository agentApprovalHistoryRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private FieldTaskRepository fieldTaskRepository;

    @Mock
    private AgronomistRepository agronomistRepository;

    @Mock
    private AgentProposalService agentProposalService;

    @Test
    void approveProposalMarksProposalItemsAssignsTasksAndSavesHistory() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);
        AgentProposalResponse expectedResponse = proposalResponse(proposalId, AgentProposalStatus.APPROVED);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agentProposalService.getAgentProposal(proposalId)).thenReturn(expectedResponse);

        AgentProposalResponse response = service().approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved for tomorrow visit")
        );

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.APPROVED);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.ASSIGNED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(fieldTask.getAssignedAgronomistId()).isEqualTo(agronomistId);

        ArgumentCaptor<TaskAssignment> assignmentCaptor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getFieldTaskId()).isEqualTo(fieldTask.getId());
        assertThat(assignmentCaptor.getValue().getAgronomistId()).isEqualTo(agronomistId);
        assertThat(assignmentCaptor.getValue().getProposalId()).isEqualTo(proposalId);
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(TaskAssignmentStatus.ACTIVE);

        ArgumentCaptor<AgentApprovalHistory> historyCaptor = ArgumentCaptor.forClass(AgentApprovalHistory.class);
        verify(agentApprovalHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(AgentApprovalAction.APPROVED);
        assertThat(historyCaptor.getValue().getReviewedBy()).isEqualTo("manager-001");
        assertThat(historyCaptor.getValue().getReason()).isEqualTo("Approved for tomorrow visit");
    }

    @Test
    void rejectProposalMarksItemsRejectedAndReturnsTasksToCreated() {
        UUID proposalId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, UUID.randomUUID());
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), UUID.randomUUID());
        AgentProposalResponse expectedResponse = proposalResponse(proposalId, AgentProposalStatus.REJECTED);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agentProposalService.getAgentProposal(proposalId)).thenReturn(expectedResponse);

        AgentProposalResponse response = service().rejectProposal(
                proposalId,
                new RejectAgentProposalRequest("manager-001", "Schedule is not suitable")
        );

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.REJECTED);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.REJECTED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.CREATED);
        assertThat(fieldTask.getAssignedAgronomistId()).isNull();

        ArgumentCaptor<AgentApprovalHistory> historyCaptor = ArgumentCaptor.forClass(AgentApprovalHistory.class);
        verify(agentApprovalHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(AgentApprovalAction.REJECTED);
        assertThat(historyCaptor.getValue().getReason()).isEqualTo("Schedule is not suitable");
    }

    @Test
    void overrideProposalUsesNewAgronomistAndAssignsTasks() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);
        Agronomist newAgronomist = agronomist(newAgronomistId, AvailabilityStatus.AVAILABLE);
        AgentProposalResponse expectedResponse = proposalResponse(proposalId, AgentProposalStatus.OVERRIDDEN);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(agronomistRepository.findById(newAgronomistId)).thenReturn(Optional.of(newAgronomist));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agentProposalService.getAgentProposal(proposalId)).thenReturn(expectedResponse);

        AgentProposalResponse response = service().overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Budi is unavailable, assign to Sari instead",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        );

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.OVERRIDDEN);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.ASSIGNED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(fieldTask.getAssignedAgronomistId()).isEqualTo(newAgronomistId);

        ArgumentCaptor<TaskAssignment> assignmentCaptor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getAgronomistId()).isEqualTo(newAgronomistId);

        ArgumentCaptor<AgentApprovalHistory> historyCaptor = ArgumentCaptor.forClass(AgentApprovalHistory.class);
        verify(agentApprovalHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(AgentApprovalAction.OVERRIDDEN);
        assertThat(historyCaptor.getValue().getReason()).isEqualTo("Budi is unavailable, assign to Sari instead");
    }

    private AgentApprovalService service() {
        return new AgentApprovalService(
                agentProposalRepository,
                agentProposalItemRepository,
                agentApprovalHistoryRepository,
                taskAssignmentRepository,
                fieldTaskRepository,
                agronomistRepository,
                agentProposalService
        );
    }

    private AgentProposal proposal(UUID id, AgentProposalStatus status) {
        AgentProposal proposal = new AgentProposal(
                UUID.randomUUID(),
                "FIELD_TASK_ASSIGNMENT",
                status,
                "Aceh Besar",
                LocalDate.of(2026, 5, 9),
                "Generated scheduling recommendations"
        );
        ReflectionTestUtils.setField(proposal, "id", id);
        ReflectionTestUtils.setField(proposal, "createdAt", Instant.now());
        ReflectionTestUtils.setField(proposal, "updatedAt", Instant.now());
        return proposal;
    }

    private AgentProposalItem proposalItem(UUID proposalId, UUID fieldTaskId, UUID agronomistId) {
        AgentProposalItem item = new AgentProposalItem(
                proposalId,
                fieldTaskId,
                agronomistId,
                "Recommended by scheduling agent",
                new BigDecimal("0.9500"),
                AgentProposalItemStatus.PROPOSED
        );
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(item, "createdAt", Instant.now());
        ReflectionTestUtils.setField(item, "updatedAt", Instant.now());
        return item;
    }

    private FieldTask fieldTask(TaskStatus status, UUID assignedAgronomistId) {
        FieldTask fieldTask = new FieldTask(
                UUID.randomUUID(),
                TaskType.WATER_LEVEL_CHECK,
                TaskPriority.HIGH,
                status,
                LocalDate.of(2026, 5, 9),
                assignedAgronomistId,
                null
        );
        ReflectionTestUtils.setField(fieldTask, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(fieldTask, "createdAt", Instant.now());
        ReflectionTestUtils.setField(fieldTask, "updatedAt", Instant.now());
        return fieldTask;
    }

    private Agronomist agronomist(UUID id, AvailabilityStatus status) {
        Agronomist agronomist = new Agronomist(
                "Sari",
                "08123456789",
                "Aceh Besar",
                5,
                status
        );
        ReflectionTestUtils.setField(agronomist, "id", id);
        ReflectionTestUtils.setField(agronomist, "createdAt", Instant.now());
        ReflectionTestUtils.setField(agronomist, "updatedAt", Instant.now());
        return agronomist;
    }

    private AgentProposalResponse proposalResponse(UUID proposalId, AgentProposalStatus status) {
        return new AgentProposalResponse(
                proposalId,
                UUID.randomUUID(),
                "FIELD_TASK_ASSIGNMENT",
                status,
                "Aceh Besar",
                LocalDate.of(2026, 5, 9),
                "Generated scheduling recommendations",
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }
}
