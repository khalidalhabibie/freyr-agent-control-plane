package com.khalid.freyr.approval;

import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.assignment.TaskAssignment;
import com.khalid.freyr.assignment.TaskAssignmentRepository;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.FieldTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgentApprovalService {

    private final AgentProposalRepository agentProposalRepository;
    private final AgentProposalItemRepository agentProposalItemRepository;
    private final AgentApprovalHistoryRepository agentApprovalHistoryRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final FieldTaskRepository fieldTaskRepository;
    private final AgronomistRepository agronomistRepository;
    private final AgentProposalService agentProposalService;

    public AgentApprovalService(
            AgentProposalRepository agentProposalRepository,
            AgentProposalItemRepository agentProposalItemRepository,
            AgentApprovalHistoryRepository agentApprovalHistoryRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            FieldTaskRepository fieldTaskRepository,
            AgronomistRepository agronomistRepository,
            AgentProposalService agentProposalService
    ) {
        this.agentProposalRepository = agentProposalRepository;
        this.agentProposalItemRepository = agentProposalItemRepository;
        this.agentApprovalHistoryRepository = agentApprovalHistoryRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.fieldTaskRepository = fieldTaskRepository;
        this.agronomistRepository = agronomistRepository;
        this.agentProposalService = agentProposalService;
    }

    @Transactional
    public AgentProposalResponse approveProposal(UUID proposalId, ApproveAgentProposalRequest request) {
        AgentProposal proposal = getPendingProposal(proposalId);
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);

        proposal.markApproved();
        agentProposalRepository.save(proposal);

        for (AgentProposalItem item : items) {
            item.markApproved();
            createAssignmentAndMarkTaskAssigned(proposalId, item);
            item.markAssigned();
            agentProposalItemRepository.save(item);
        }

        saveHistory(proposalId, AgentApprovalAction.APPROVED, request.reviewedBy(), request.note());
        return agentProposalService.getAgentProposal(proposalId);
    }

    @Transactional
    public AgentProposalResponse rejectProposal(UUID proposalId, RejectAgentProposalRequest request) {
        AgentProposal proposal = getPendingProposal(proposalId);
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);

        proposal.markRejected();
        agentProposalRepository.save(proposal);

        for (AgentProposalItem item : items) {
            item.markRejected();
            agentProposalItemRepository.save(item);

            FieldTask fieldTask = getFieldTask(item.getFieldTaskId());
            fieldTask.markCreated();
            fieldTaskRepository.save(fieldTask);
        }

        saveHistory(proposalId, AgentApprovalAction.REJECTED, request.reviewedBy(), request.reason());
        return agentProposalService.getAgentProposal(proposalId);
    }

    @Transactional
    public AgentProposalResponse overrideProposal(UUID proposalId, OverrideAgentProposalRequest request) {
        AgentProposal proposal = getPendingProposal(proposalId);
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);
        Map<UUID, UUID> overrides = request.overrides()
                .stream()
                .collect(Collectors.toMap(
                        OverrideAgentProposalItemRequest::proposalItemId,
                        OverrideAgentProposalItemRequest::newAgronomistId
                ));

        validateOverrideItemsBelongToProposal(items, overrides);
        validateOverrideAgronomists(overrides);

        proposal.markOverridden();
        agentProposalRepository.save(proposal);

        for (AgentProposalItem item : items) {
            UUID overriddenAgronomistId = overrides.get(item.getId());
            if (overriddenAgronomistId != null) {
                item.markOverridden(overriddenAgronomistId);
            }

            createAssignmentAndMarkTaskAssigned(proposalId, item);
            item.markAssigned();
            agentProposalItemRepository.save(item);
        }

        saveHistory(proposalId, AgentApprovalAction.OVERRIDDEN, request.reviewedBy(), request.reason());
        return agentProposalService.getAgentProposal(proposalId);
    }

    private AgentProposal getPendingProposal(UUID proposalId) {
        AgentProposal proposal = agentProposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Agent proposal not found"));

        if (proposal.getStatus() != AgentProposalStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Agent proposal is not pending approval");
        }

        return proposal;
    }

    private void createAssignmentAndMarkTaskAssigned(UUID proposalId, AgentProposalItem item) {
        taskAssignmentRepository.save(new TaskAssignment(
                item.getFieldTaskId(),
                item.getRecommendedAgronomistId(),
                proposalId
        ));

        FieldTask fieldTask = getFieldTask(item.getFieldTaskId());
        fieldTask.markAssigned(item.getRecommendedAgronomistId());
        fieldTaskRepository.save(fieldTask);
    }

    private FieldTask getFieldTask(UUID fieldTaskId) {
        return fieldTaskRepository.findById(fieldTaskId)
                .orElseThrow(() -> new EntityNotFoundException("Field task not found"));
    }

    private void validateOverrideItemsBelongToProposal(List<AgentProposalItem> items, Map<UUID, UUID> overrides) {
        List<UUID> itemIds = items.stream()
                .map(AgentProposalItem::getId)
                .toList();

        for (UUID proposalItemId : overrides.keySet()) {
            if (!itemIds.contains(proposalItemId)) {
                throw new EntityNotFoundException("Agent proposal item not found");
            }
        }
    }

    private void validateOverrideAgronomists(Map<UUID, UUID> overrides) {
        for (UUID agronomistId : overrides.values()) {
            Agronomist agronomist = agronomistRepository.findById(agronomistId)
                    .orElseThrow(() -> new EntityNotFoundException("Agronomist not found"));
            if (agronomist.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                throw new IllegalArgumentException("Agronomist is not available");
            }
        }
    }

    private void saveHistory(
            UUID proposalId,
            AgentApprovalAction action,
            String reviewedBy,
            String reason
    ) {
        agentApprovalHistoryRepository.save(new AgentApprovalHistory(
                proposalId,
                action,
                reviewedBy,
                reason
        ));
    }
}
