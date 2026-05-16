package com.khalid.freyr.approval;

import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.assignment.TaskAssignment;
import com.khalid.freyr.assignment.TaskAssignmentRepository;
import com.khalid.freyr.assignment.TaskAssignmentStatus;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.FieldTaskRepository;
import com.khalid.freyr.fieldtask.TaskStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    public AgentApprovalService(
            AgentProposalRepository agentProposalRepository,
            AgentProposalItemRepository agentProposalItemRepository,
            AgentApprovalHistoryRepository agentApprovalHistoryRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            FieldTaskRepository fieldTaskRepository,
            AgronomistRepository agronomistRepository
    ) {
        this.agentProposalRepository = agentProposalRepository;
        this.agentProposalItemRepository = agentProposalItemRepository;
        this.agentApprovalHistoryRepository = agentApprovalHistoryRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.fieldTaskRepository = fieldTaskRepository;
        this.agronomistRepository = agronomistRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentProposalSummaryResponse> getProposals() {
        return agentProposalRepository.findAll()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentProposalDetailResponse getProposal(UUID proposalId) {
        AgentProposal proposal = agentProposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Agent proposal not found"));
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);

        return toDetailResponse(proposal, items);
    }

    @Transactional
    public AgentProposalDetailResponse approveProposal(UUID proposalId, ApproveAgentProposalRequest request) {
        AgentProposal proposal = getPendingProposal(proposalId);
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);

        validateProposalIsFresh(proposal, items);
        validateAgronomistAvailabilityAndCapacity(proposal, items);
        validateNoActiveAssignments(items);

        proposal.markApproved();
        agentProposalRepository.save(proposal);

        for (AgentProposalItem item : items) {
            createAssignmentAndMarkTaskAssigned(proposalId, item);
            item.markAssigned();
            agentProposalItemRepository.save(item);
        }

        saveHistory(proposalId, AgentApprovalAction.APPROVED, request.reviewedBy(), request.note());
        return toDetailResponse(proposal, items);
    }

    @Transactional
    public AgentProposalDetailResponse rejectProposal(UUID proposalId, RejectAgentProposalRequest request) {
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
        return toDetailResponse(proposal, items);
    }

    @Transactional
    public AgentProposalDetailResponse overrideProposal(UUID proposalId, OverrideAgentProposalRequest request) {
        AgentProposal proposal = getPendingProposal(proposalId);
        List<AgentProposalItem> items = agentProposalItemRepository.findByProposalId(proposalId);
        validateNoDuplicateOverrideItems(request.overrides());
        Map<UUID, UUID> overrides = request.overrides()
                .stream()
                .collect(Collectors.toMap(
                        OverrideAgentProposalItemRequest::proposalItemId,
                        OverrideAgentProposalItemRequest::newAgronomistId
                ));

        validateOverrideItemsBelongToProposal(items, overrides);
        validateProposalIsFresh(proposal, items);
        validateOverrideAgronomistAvailabilityAndCapacity(proposal, items, overrides);
        validateNoActiveAssignments(items);

        proposal.markOverridden();
        agentProposalRepository.save(proposal);

        for (AgentProposalItem item : items) {
            UUID newAgronomistId = overrides.get(item.getId());
            if (newAgronomistId != null) {
                item.overrideAgronomist(newAgronomistId, request.reason());
            }
            createAssignmentAndMarkTaskAssigned(proposalId, item);
            if (!item.isOverridden()) {
                item.markAssigned();
            }
            agentProposalItemRepository.save(item);
        }

        saveHistory(proposalId, AgentApprovalAction.OVERRIDDEN, request.reviewedBy(), request.reason());
        return toDetailResponse(proposal, items);
    }

    private AgentProposal getPendingProposal(UUID proposalId) {
        AgentProposal proposal = agentProposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Agent proposal not found"));

        if (proposal.getStatus() != AgentProposalStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Agent proposal is not pending approval");
        }

        return proposal;
    }

    private void validateNoDuplicateOverrideItems(List<OverrideAgentProposalItemRequest> overrides) {
        Set<UUID> proposalItemIds = new HashSet<>();
        for (OverrideAgentProposalItemRequest override : overrides) {
            UUID proposalItemId = override.proposalItemId();
            if (!proposalItemIds.add(proposalItemId)) {
                throw new IllegalArgumentException("Duplicate override for proposal item: " + proposalItemId);
            }
        }
    }

    private void validateNoActiveAssignments(List<AgentProposalItem> items) {
        for (AgentProposalItem item : items) {
            if (hasActiveAssignment(item.getFieldTaskId())) {
                throw new IllegalArgumentException(
                        "Field task already has an active assignment: " + item.getFieldTaskId()
                );
            }
        }
    }

    private void validateProposalIsFresh(AgentProposal proposal, List<AgentProposalItem> items) {
        if (proposal.getScheduleDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Agent proposal schedule date is in the past: " + proposal.getScheduleDate()
            );
        }

        for (AgentProposalItem item : items) {
            FieldTask fieldTask = getFieldTask(item.getFieldTaskId());
            validateFieldTaskIsStillProposed(fieldTask);
        }
    }

    private void validateFieldTaskIsStillProposed(FieldTask fieldTask) {
        if (fieldTask.getAssignedAgronomistId() != null || fieldTask.getStatus() == TaskStatus.ASSIGNED) {
            throw new IllegalArgumentException("Field task is already assigned: " + fieldTask.getId());
        }

        if (fieldTask.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Field task is already completed: " + fieldTask.getId());
        }

        if (fieldTask.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Field task is cancelled: " + fieldTask.getId());
        }

        if (fieldTask.getStatus() != TaskStatus.PROPOSED) {
            throw new IllegalArgumentException("Field task is no longer proposed: " + fieldTask.getId());
        }
    }

    private void createAssignmentAndMarkTaskAssigned(UUID proposalId, AgentProposalItem item) {
        if (hasActiveAssignment(item.getFieldTaskId())) {
            throw new IllegalArgumentException("Field task already has an active assignment: " + item.getFieldTaskId());
        }

        taskAssignmentRepository.save(new TaskAssignment(
                item.getFieldTaskId(),
                item.getRecommendedAgronomistId(),
                proposalId
        ));

        FieldTask fieldTask = getFieldTask(item.getFieldTaskId());
        fieldTask.markAssigned(item.getRecommendedAgronomistId());
        fieldTaskRepository.save(fieldTask);
    }

    private boolean hasActiveAssignment(UUID fieldTaskId) {
        return taskAssignmentRepository.existsByFieldTaskIdAndStatus(fieldTaskId, TaskAssignmentStatus.ACTIVE);
    }

    private FieldTask getFieldTask(UUID fieldTaskId) {
        return fieldTaskRepository.findById(fieldTaskId)
                .orElseThrow(() -> new EntityNotFoundException("Field task not found: " + fieldTaskId));
    }

    private void validateAgronomistAvailabilityAndCapacity(AgentProposal proposal, List<AgentProposalItem> items) {
        validateAgronomistIdsAvailabilityAndCapacity(
                proposal,
                items.stream()
                        .map(AgentProposalItem::getRecommendedAgronomistId)
                        .toList()
        );
    }

    private void validateOverrideAgronomistAvailabilityAndCapacity(
            AgentProposal proposal,
            List<AgentProposalItem> items,
            Map<UUID, UUID> overrides
    ) {
        validateAgronomistIdsAvailabilityAndCapacity(
                proposal,
                items.stream()
                        .map(item -> overrides.getOrDefault(item.getId(), item.getRecommendedAgronomistId()))
                        .toList()
        );
    }

    private void validateAgronomistIdsAvailabilityAndCapacity(AgentProposal proposal, List<UUID> agronomistIds) {
        Map<UUID, Integer> proposedAssignmentCounts = new HashMap<>();

        for (UUID agronomistId : agronomistIds) {
            Agronomist agronomist = agronomistRepository.findById(agronomistId)
                    .orElseThrow(() -> new EntityNotFoundException("Agronomist not found"));

            if (agronomist.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                throw new IllegalArgumentException("Agronomist is not available: " + agronomistId);
            }

            long activeAssignments = taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                    agronomistId,
                    proposal.getScheduleDate()
            );
            int proposedAssignments = proposedAssignmentCounts.getOrDefault(agronomistId, 0) + 1;

            if (activeAssignments + proposedAssignments > agronomist.getMaxDailyVisit()) {
                throw new IllegalArgumentException("Agronomist daily capacity is full: " + agronomistId);
            }

            proposedAssignmentCounts.put(agronomistId, proposedAssignments);
        }
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

    private void saveHistory(UUID proposalId, AgentApprovalAction action, String reviewedBy, String reason) {
        agentApprovalHistoryRepository.save(new AgentApprovalHistory(
                proposalId,
                action,
                reviewedBy,
                reason
        ));
    }

    private AgentProposalSummaryResponse toSummaryResponse(AgentProposal proposal) {
        return new AgentProposalSummaryResponse(
                proposal.getId(),
                proposal.getExecutionId(),
                proposal.getProposalType(),
                proposal.getStatus(),
                proposal.getDistrict(),
                proposal.getScheduleDate(),
                proposal.getSummary(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }

    private AgentProposalDetailResponse toDetailResponse(AgentProposal proposal, List<AgentProposalItem> items) {
        return new AgentProposalDetailResponse(
                proposal.getId(),
                proposal.getExecutionId(),
                proposal.getProposalType(),
                proposal.getStatus(),
                proposal.getDistrict(),
                proposal.getScheduleDate(),
                proposal.getSummary(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt(),
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private AgentProposalItemResponse toItemResponse(AgentProposalItem item) {
        return new AgentProposalItemResponse(
                item.getId(),
                item.getProposalId(),
                item.getFieldTaskId(),
                item.getRecommendedAgronomistId(),
                item.isOverridden(),
                item.getOriginalAgronomistId(),
                item.getOverriddenAgronomistId(),
                item.getOverrideReason(),
                item.getOverriddenAt(),
                item.getRecommendationReason(),
                item.getConfidenceScore(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
