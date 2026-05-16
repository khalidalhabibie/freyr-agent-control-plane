package com.khalid.freyr.agent.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khalid.freyr.agent.execution.AgentExecutionResponse;
import com.khalid.freyr.agent.execution.AgentExecutionService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SchedulingProposalWriter {

    private static final String PROPOSAL_TYPE = "SCHEDULING";

    private final FieldTaskRepository fieldTaskRepository;
    private final AgronomistRepository agronomistRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final AgentProposalRepository agentProposalRepository;
    private final AgentProposalItemRepository agentProposalItemRepository;
    private final AgentExecutionService agentExecutionService;
    private final SchedulingAgent schedulingAgent;
    private final ObjectMapper objectMapper;

    public SchedulingProposalWriter(
            FieldTaskRepository fieldTaskRepository,
            AgronomistRepository agronomistRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            AgentProposalRepository agentProposalRepository,
            AgentProposalItemRepository agentProposalItemRepository,
            AgentExecutionService agentExecutionService,
            SchedulingAgent schedulingAgent,
            ObjectMapper objectMapper
    ) {
        this.fieldTaskRepository = fieldTaskRepository;
        this.agronomistRepository = agronomistRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.agentProposalRepository = agentProposalRepository;
        this.agentProposalItemRepository = agentProposalItemRepository;
        this.agentExecutionService = agentExecutionService;
        this.schedulingAgent = schedulingAgent;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RunSchedulingAgentResponse generateProposal(
            RunSchedulingAgentRequest request,
            AgentExecutionResponse execution
    ) {
        List<FieldTask> eligibleTasks = fieldTaskRepository.findEligibleUnassignedTasksByDistrictAndDueDateWindow(
                request.district(),
                request.scheduleDate().plusDays(1)
        );
        if (eligibleTasks.isEmpty()) {
            throw new SchedulingAgentException("No eligible field tasks found for scheduling");
        }

        List<Agronomist> availableAgronomists = agronomistRepository.findByAssignedDistrictAndAvailabilityStatus(
                request.district(),
                AvailabilityStatus.AVAILABLE
        );
        if (availableAgronomists.isEmpty()) {
            throw new SchedulingAgentException("No available agronomists found for scheduling");
        }

        Map<UUID, Long> activeAssignmentCounts = activeAssignmentCounts(availableAgronomists, request);
        List<SchedulingRecommendation> recommendations = schedulingAgent.recommendAssignments(
                eligibleTasks,
                availableAgronomists,
                request.scheduleDate(),
                activeAssignmentCounts
        );
        if (recommendations.isEmpty()) {
            throw new SchedulingAgentException("No scheduling recommendations could be generated within capacity");
        }

        AgentProposal proposal = agentProposalRepository.save(new AgentProposal(
                execution.id(),
                PROPOSAL_TYPE,
                AgentProposalStatus.PENDING_APPROVAL,
                request.district(),
                request.scheduleDate(),
                "Rule-based scheduling proposal for " + request.district()
        ));

        for (SchedulingRecommendation recommendation : recommendations) {
            agentProposalItemRepository.save(new AgentProposalItem(
                    proposal.getId(),
                    recommendation.fieldTask().getId(),
                    recommendation.agronomistId(),
                    recommendation.reason(),
                    recommendation.confidenceScore(),
                    AgentProposalItemStatus.PROPOSED
            ));

            recommendation.fieldTask().markProposed();
            fieldTaskRepository.save(recommendation.fieldTask());
        }

        agentExecutionService.markExecutionSuccess(execution.id(), outputPayload(proposal, recommendations));

        return new RunSchedulingAgentResponse(
                execution.id(),
                proposal.getId(),
                proposal.getStatus(),
                "Scheduling proposal has been generated and requires approval"
        );
    }

    private Map<UUID, Long> activeAssignmentCounts(
            List<Agronomist> agronomists,
            RunSchedulingAgentRequest request
    ) {
        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (Agronomist agronomist : agronomists) {
            counts.put(
                    agronomist.getId(),
                    taskAssignmentRepository.countActiveAssignmentsForAgronomistOnScheduleDate(
                            agronomist.getId(),
                            request.scheduleDate()
                    )
            );
        }
        return counts;
    }

    private String outputPayload(AgentProposal proposal, List<SchedulingRecommendation> recommendations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("proposalId", proposal.getId());
        payload.put("recommendationCount", recommendations.size());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize scheduling output payload", exception);
        }
    }
}
