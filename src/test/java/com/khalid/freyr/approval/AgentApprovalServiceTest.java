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
import org.mockito.InjectMocks;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @InjectMocks
    private AgentApprovalService agentApprovalService;

    @Test
    void getProposalsReturnsSummaryResponsesWithoutItems() {
        UUID proposalId = UUID.randomUUID();
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);

        when(agentProposalRepository.findAll()).thenReturn(List.of(proposal));

        List<AgentProposalSummaryResponse> response = agentApprovalService.getProposals();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(proposalId);
        assertThat(response.getFirst().executionId()).isEqualTo(proposal.getExecutionId());
        assertThat(response.getFirst().proposalType()).isEqualTo(proposal.getProposalType());
        assertThat(response.getFirst().status()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(response.getFirst().district()).isEqualTo(proposal.getDistrict());
        assertThat(response.getFirst().scheduleDate()).isEqualTo(proposal.getScheduleDate());
        assertThat(response.getFirst().summary()).isEqualTo(proposal.getSummary());
        verify(agentProposalItemRepository, never()).findByProposalId(proposalId);
    }

    @Test
    void getProposalReturnsDetailResponseWithItems() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));

        AgentProposalDetailResponse response = agentApprovalService.getProposal(proposalId);

        assertThat(response.id()).isEqualTo(proposalId);
        assertThat(response.status()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(item.getId());
        assertThat(response.items().getFirst().fieldTaskId()).isEqualTo(fieldTask.getId());
        assertThat(response.items().getFirst().recommendedAgronomistId()).isEqualTo(agronomistId);
    }

    @Test
    void approveProposalAssignsItemsTasksAndSavesHistory() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);
        Agronomist agronomist = agronomist(agronomistId, AvailabilityStatus.AVAILABLE, 2);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(agronomistId)).thenReturn(Optional.of(agronomist));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                agronomistId,
                proposal.getScheduleDate()
        )).thenReturn(1L);
        when(taskAssignmentRepository.existsByFieldTaskIdAndStatus(
                fieldTask.getId(),
                TaskAssignmentStatus.ACTIVE
        )).thenReturn(false);

        AgentProposalDetailResponse response = agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved for tomorrow visit")
        );

        assertThat(response.status()).isEqualTo(AgentProposalStatus.APPROVED);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo(AgentProposalItemStatus.ASSIGNED);
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
    void approveProposalFailsWhenFieldTaskAlreadyHasActiveAssignment() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);
        Agronomist agronomist = agronomist(agronomistId, AvailabilityStatus.AVAILABLE, 2);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(agronomistId)).thenReturn(Optional.of(agronomist));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                agronomistId,
                proposal.getScheduleDate()
        )).thenReturn(0L);
        when(taskAssignmentRepository.existsByFieldTaskIdAndStatus(
                fieldTask.getId(),
                TaskAssignmentStatus.ACTIVE
        )).thenReturn(true);

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field task already has an active assignment: " + fieldTask.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenRecommendedAgronomistIsUnavailable() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);
        Agronomist agronomist = agronomist(agronomistId, AvailabilityStatus.UNAVAILABLE, 2);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(agronomistId)).thenReturn(Optional.of(agronomist));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agronomist is not available: " + agronomistId);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenRecommendedAgronomistCapacityIsFull() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);
        Agronomist agronomist = agronomist(agronomistId, AvailabilityStatus.AVAILABLE, 1);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(agronomistId)).thenReturn(Optional.of(agronomist));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                agronomistId,
                proposal.getScheduleDate()
        )).thenReturn(1L);

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agronomist daily capacity is full: " + agronomistId);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.PROPOSED);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenFieldTaskIsAlreadyAssigned() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.ASSIGNED, agronomistId);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field task is already assigned: " + fieldTask.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenFieldTaskIsCompleted() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.COMPLETED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field task is already completed: " + fieldTask.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenFieldTaskIsCancelled() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.CANCELLED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field task is cancelled: " + fieldTask.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenScheduleDateIsInThePast() {
        UUID proposalId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        LocalDate pastScheduleDate = LocalDate.now().minusDays(1);
        ReflectionTestUtils.setField(proposal, "scheduleDate", pastScheduleDate);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), agronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent proposal schedule date is in the past: " + pastScheduleDate);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(fieldTaskRepository, never()).findById(fieldTask.getId());
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void rejectProposalRejectsItemsReturnsTasksToCreatedAndSavesHistory() {
        UUID proposalId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, UUID.randomUUID());
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), UUID.randomUUID());

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));

        AgentProposalDetailResponse response = agentApprovalService.rejectProposal(
                proposalId,
                new RejectAgentProposalRequest("manager-001", "Schedule is not suitable")
        );

        assertThat(response.status()).isEqualTo(AgentProposalStatus.REJECTED);
        assertThat(response.items().getFirst().status()).isEqualTo(AgentProposalItemStatus.REJECTED);
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
    void overrideProposalUsesNewAgronomistForOverriddenItemsAndAssignsTasks() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        UUID unchangedAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        FieldTask unchangedFieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);
        AgentProposalItem unchangedItem = proposalItem(proposalId, unchangedFieldTask.getId(), unchangedAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item, unchangedItem));
        when(agronomistRepository.findById(newAgronomistId))
                .thenReturn(Optional.of(agronomist(newAgronomistId, AvailabilityStatus.AVAILABLE, 2)));
        when(agronomistRepository.findById(unchangedAgronomistId))
                .thenReturn(Optional.of(agronomist(unchangedAgronomistId, AvailabilityStatus.AVAILABLE, 2)));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                newAgronomistId,
                proposal.getScheduleDate()
        )).thenReturn(0L);
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                unchangedAgronomistId,
                proposal.getScheduleDate()
        )).thenReturn(0L);
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(fieldTaskRepository.findById(unchangedFieldTask.getId())).thenReturn(Optional.of(unchangedFieldTask));

        AgentProposalDetailResponse response = agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Budi is unavailable, assign to Sari instead",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        );

        assertThat(response.status()).isEqualTo(AgentProposalStatus.OVERRIDDEN);
        assertThat(response.items()).hasSize(2);
        AgentProposalItemResponse overriddenItemResponse = response.items().getFirst();
        AgentProposalItemResponse unchangedItemResponse = response.items().get(1);
        assertThat(overriddenItemResponse.recommendedAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(overriddenItemResponse.overridden()).isTrue();
        assertThat(overriddenItemResponse.originalAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(overriddenItemResponse.overriddenAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(overriddenItemResponse.overrideReason())
                .isEqualTo("Budi is unavailable, assign to Sari instead");
        assertThat(overriddenItemResponse.overriddenAt()).isNotNull();
        assertThat(overriddenItemResponse.status()).isEqualTo(AgentProposalItemStatus.OVERRIDDEN);
        assertThat(unchangedItemResponse.recommendedAgronomistId()).isEqualTo(unchangedAgronomistId);
        assertThat(unchangedItemResponse.overridden()).isFalse();
        assertThat(unchangedItemResponse.originalAgronomistId()).isNull();
        assertThat(unchangedItemResponse.overriddenAgronomistId()).isNull();
        assertThat(unchangedItemResponse.overrideReason()).isNull();
        assertThat(unchangedItemResponse.overriddenAt()).isNull();
        assertThat(unchangedItemResponse.status()).isEqualTo(AgentProposalItemStatus.ASSIGNED);
        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.OVERRIDDEN);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(item.isOverridden()).isTrue();
        assertThat(item.getOriginalAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(item.getOverriddenAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(item.getOverrideReason()).isEqualTo("Budi is unavailable, assign to Sari instead");
        assertThat(item.getOverriddenAt()).isNotNull();
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.OVERRIDDEN);
        assertThat(unchangedItem.isOverridden()).isFalse();
        assertThat(unchangedItem.getStatus()).isEqualTo(AgentProposalItemStatus.ASSIGNED);
        assertThat(fieldTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(fieldTask.getAssignedAgronomistId()).isEqualTo(newAgronomistId);
        assertThat(unchangedFieldTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(unchangedFieldTask.getAssignedAgronomistId()).isEqualTo(unchangedAgronomistId);

        ArgumentCaptor<TaskAssignment> assignmentCaptor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository, times(2)).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getAllValues())
                .extracting(TaskAssignment::getAgronomistId)
                .containsExactly(newAgronomistId, unchangedAgronomistId);
        assertThat(assignmentCaptor.getAllValues())
                .extracting(TaskAssignment::getStatus)
                .containsExactly(TaskAssignmentStatus.ACTIVE, TaskAssignmentStatus.ACTIVE);

        ArgumentCaptor<AgentApprovalHistory> historyCaptor = ArgumentCaptor.forClass(AgentApprovalHistory.class);
        verify(agentApprovalHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(AgentApprovalAction.OVERRIDDEN);
    }

    @Test
    void overrideProposalThrowsWhenTargetAgronomistIsUnavailable() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(newAgronomistId))
                .thenReturn(Optional.of(agronomist(newAgronomistId, AvailabilityStatus.UNAVAILABLE, 2)));

        assertThatThrownBy(() -> agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Use another agronomist",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agronomist is not available: " + newAgronomistId);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void overrideProposalThrowsWhenTargetAgronomistCapacityIsFull() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(agronomistRepository.findById(newAgronomistId))
                .thenReturn(Optional.of(agronomist(newAgronomistId, AvailabilityStatus.AVAILABLE, 1)));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                newAgronomistId,
                proposal.getScheduleDate()
        )).thenReturn(1L);

        assertThatThrownBy(() -> agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Use another agronomist",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agronomist daily capacity is full: " + newAgronomistId);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void overrideProposalValidatesNonOverriddenProposalItemsToo() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        UUID unchangedAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        FieldTask unchangedFieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);
        AgentProposalItem unchangedItem = proposalItem(proposalId, unchangedFieldTask.getId(), unchangedAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item, unchangedItem));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));
        when(fieldTaskRepository.findById(unchangedFieldTask.getId())).thenReturn(Optional.of(unchangedFieldTask));
        when(agronomistRepository.findById(newAgronomistId))
                .thenReturn(Optional.of(agronomist(newAgronomistId, AvailabilityStatus.AVAILABLE, 2)));
        when(taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                newAgronomistId,
                proposal.getScheduleDate()
        )).thenReturn(0L);
        when(agronomistRepository.findById(unchangedAgronomistId))
                .thenReturn(Optional.of(agronomist(unchangedAgronomistId, AvailabilityStatus.UNAVAILABLE, 2)));

        assertThatThrownBy(() -> agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Use another agronomist",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agronomist is not available: " + unchangedAgronomistId);

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        assertThat(unchangedItem.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void overrideProposalThrowsWhenFieldTaskIsStale() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID newAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.COMPLETED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));
        when(fieldTaskRepository.findById(fieldTask.getId())).thenReturn(Optional.of(fieldTask));

        assertThatThrownBy(() -> agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Use another agronomist",
                        List.of(new OverrideAgentProposalItemRequest(item.getId(), newAgronomistId))
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field task is already completed: " + fieldTask.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getRecommendedAgronomistId()).isEqualTo(originalAgronomistId);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void overrideProposalThrowsClearValidationErrorForDuplicateProposalItemOverrides() {
        UUID proposalId = UUID.randomUUID();
        UUID originalAgronomistId = UUID.randomUUID();
        UUID firstNewAgronomistId = UUID.randomUUID();
        UUID secondNewAgronomistId = UUID.randomUUID();
        FieldTask fieldTask = fieldTask(TaskStatus.PROPOSED, null);
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.PENDING_APPROVAL);
        AgentProposalItem item = proposalItem(proposalId, fieldTask.getId(), originalAgronomistId);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(agentProposalItemRepository.findByProposalId(proposalId)).thenReturn(List.of(item));

        assertThatThrownBy(() -> agentApprovalService.overrideProposal(
                proposalId,
                new OverrideAgentProposalRequest(
                        "manager-001",
                        "Use another agronomist",
                        List.of(
                                new OverrideAgentProposalItemRequest(item.getId(), firstNewAgronomistId),
                                new OverrideAgentProposalItemRequest(item.getId(), secondNewAgronomistId)
                        )
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate override for proposal item: " + item.getId());

        assertThat(proposal.getStatus()).isEqualTo(AgentProposalStatus.PENDING_APPROVAL);
        assertThat(item.getStatus()).isEqualTo(AgentProposalItemStatus.PROPOSED);
        verify(agronomistRepository, never()).findById(any(UUID.class));
        verify(agentProposalRepository, never()).save(any(AgentProposal.class));
        verify(agentProposalItemRepository, never()).save(any(AgentProposalItem.class));
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void approveProposalThrowsWhenProposalIsNotPendingApproval() {
        UUID proposalId = UUID.randomUUID();
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.APPROVED);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-001", "Approved")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent proposal is not pending approval");

        verify(agentProposalItemRepository, never()).findByProposalId(proposalId);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void proposalCannotBeApprovedTwice() {
        UUID proposalId = UUID.randomUUID();
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.APPROVED);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> agentApprovalService.approveProposal(
                proposalId,
                new ApproveAgentProposalRequest("manager-002", "Approve again")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent proposal is not pending approval");

        verify(agentProposalItemRepository, never()).findByProposalId(proposalId);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    @Test
    void rejectProposalThrowsWhenProposalIsAlreadyApproved() {
        UUID proposalId = UUID.randomUUID();
        AgentProposal proposal = proposal(proposalId, AgentProposalStatus.APPROVED);

        when(agentProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> agentApprovalService.rejectProposal(
                proposalId,
                new RejectAgentProposalRequest("manager-002", "Reject after approval")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent proposal is not pending approval");

        verify(agentProposalItemRepository, never()).findByProposalId(proposalId);
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        verify(agentApprovalHistoryRepository, never()).save(any(AgentApprovalHistory.class));
    }

    private AgentProposal proposal(UUID id, AgentProposalStatus status) {
        AgentProposal proposal = new AgentProposal(
                UUID.randomUUID(),
                "FIELD_TASK_ASSIGNMENT",
                status,
                "Aceh Besar",
                LocalDate.now().plusDays(1),
                "Generated scheduling recommendations"
        );
        ReflectionTestUtils.setField(proposal, "id", id);
        ReflectionTestUtils.setField(proposal, "version", 0L);
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
                LocalDate.now().plusDays(1),
                assignedAgronomistId,
                null
        );
        ReflectionTestUtils.setField(fieldTask, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(fieldTask, "createdAt", Instant.now());
        ReflectionTestUtils.setField(fieldTask, "updatedAt", Instant.now());
        return fieldTask;
    }

    private Agronomist agronomist(UUID id, AvailabilityStatus status, int maxDailyVisit) {
        Agronomist agronomist = new Agronomist(
                "Sari",
                "08123456789",
                "Aceh Besar",
                maxDailyVisit,
                status
        );
        ReflectionTestUtils.setField(agronomist, "id", id);
        ReflectionTestUtils.setField(agronomist, "createdAt", Instant.now());
        ReflectionTestUtils.setField(agronomist, "updatedAt", Instant.now());
        return agronomist;
    }
}
