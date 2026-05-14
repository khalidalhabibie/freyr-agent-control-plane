package com.khalid.freyr.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.approval.AgentProposal;
import com.khalid.freyr.approval.AgentProposalItem;
import com.khalid.freyr.approval.AgentProposalItemRepository;
import com.khalid.freyr.approval.AgentProposalItemStatus;
import com.khalid.freyr.approval.AgentProposalRepository;
import com.khalid.freyr.approval.AgentProposalStatus;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.FieldTaskRepository;
import com.khalid.freyr.fieldtask.TaskPriority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuleBasedSchedulingAgent implements SchedulingAgent {

    private static final String AGENT_NAME = "rule-based-scheduling-agent";
    private static final String EXECUTION_TYPE = "SCHEDULING_PROPOSAL";
    private static final String MODEL_NAME = "rule-based";
    private static final String PROMPT_VERSION = "rules-v1";
    private static final String PROPOSAL_TYPE = "FIELD_TASK_ASSIGNMENT";
    private static final String SUCCESS_MESSAGE = "Scheduling proposal has been generated and requires approval";

    private final AgentExecutionRepository agentExecutionRepository;
    private final FieldTaskRepository fieldTaskRepository;
    private final AgronomistRepository agronomistRepository;
    private final AgentProposalRepository agentProposalRepository;
    private final AgentProposalItemRepository agentProposalItemRepository;
    private final ObjectMapper objectMapper;

    public RuleBasedSchedulingAgent(
            AgentExecutionRepository agentExecutionRepository,
            FieldTaskRepository fieldTaskRepository,
            AgronomistRepository agronomistRepository,
            AgentProposalRepository agentProposalRepository,
            AgentProposalItemRepository agentProposalItemRepository,
            ObjectMapper objectMapper
    ) {
        this.agentExecutionRepository = agentExecutionRepository;
        this.fieldTaskRepository = fieldTaskRepository;
        this.agronomistRepository = agronomistRepository;
        this.agentProposalRepository = agentProposalRepository;
        this.agentProposalItemRepository = agentProposalItemRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RunSchedulingAgentResponse run(RunSchedulingAgentRequest request) {
        AgentExecution execution = agentExecutionRepository.save(new AgentExecution(
                AGENT_NAME,
                EXECUTION_TYPE,
                toJson(Map.of("district", request.district(), "scheduleDate", request.scheduleDate())),
                null,
                AgentExecutionStatus.RUNNING,
                MODEL_NAME,
                PROMPT_VERSION,
                null,
                Instant.now(),
                null
        ));

        try {
            List<FieldTask> tasks = fieldTaskRepository.findUnassignedTasksByDistrictAndDueDate(
                    request.district(),
                    request.scheduleDate()
            );
            List<Agronomist> agronomists = agronomistRepository.findByAssignedDistrictAndAvailabilityStatus(
                    request.district(),
                    AvailabilityStatus.AVAILABLE
            );

            if (agronomists.isEmpty()) {
                return failExecution(execution, "No available agronomist found for district " + request.district());
            }

            List<RecommendedAssignment> assignments = recommendAssignments(tasks, agronomists, request.scheduleDate());
            AgentProposal proposal = agentProposalRepository.save(new AgentProposal(
                    execution.getId(),
                    PROPOSAL_TYPE,
                    AgentProposalStatus.PENDING_APPROVAL,
                    request.district(),
                    request.scheduleDate(),
                    "Generated " + assignments.size() + " scheduling recommendation(s)"
            ));

            for (RecommendedAssignment assignment : assignments) {
                agentProposalItemRepository.save(new AgentProposalItem(
                        proposal.getId(),
                        assignment.task().getId(),
                        assignment.agronomist().getId(),
                        assignment.reason(),
                        assignment.confidenceScore(),
                        AgentProposalItemStatus.PROPOSED
                ));
                assignment.task().markProposed();
                fieldTaskRepository.save(assignment.task());
            }

            execution.markSuccess(toJson(Map.of(
                    "proposalId", proposal.getId(),
                    "recommendedAssignments", assignments.size(),
                    "district", request.district(),
                    "scheduleDate", request.scheduleDate()
            )));
            agentExecutionRepository.save(execution);

            return new RunSchedulingAgentResponse(
                    execution.getId(),
                    proposal.getId(),
                    AgentProposalStatus.PENDING_APPROVAL.name(),
                    SUCCESS_MESSAGE
            );
        } catch (RuntimeException exception) {
            return failExecution(execution, exception.getMessage());
        }
    }

    private RunSchedulingAgentResponse failExecution(AgentExecution execution, String message) {
        execution.markFailed(message);
        agentExecutionRepository.save(execution);
        return new RunSchedulingAgentResponse(
                execution.getId(),
                null,
                AgentExecutionStatus.FAILED.name(),
                message
        );
    }

    private List<RecommendedAssignment> recommendAssignments(
            List<FieldTask> tasks,
            List<Agronomist> agronomists,
            LocalDate scheduleDate
    ) {
        List<FieldTask> orderedTasks = tasks.stream()
                .sorted(Comparator.comparing(RuleBasedSchedulingAgent::priorityRank).reversed()
                        .thenComparing(FieldTask::getDueDate))
                .toList();
        Map<UUID, Integer> assignmentCounts = new HashMap<>();
        List<RecommendedAssignment> assignments = new ArrayList<>();

        for (FieldTask task : orderedTasks) {
            agronomists.stream()
                    .filter(agronomist -> hasCapacity(agronomist, assignmentCounts))
                    .max(Comparator.comparingInt(agronomist -> score(task, agronomist, assignmentCounts, scheduleDate)))
                    .ifPresent(agronomist -> {
                        int score = score(task, agronomist, assignmentCounts, scheduleDate);
                        assignmentCounts.merge(agronomist.getId(), 1, Integer::sum);
                        assignments.add(new RecommendedAssignment(
                                task,
                                agronomist,
                                recommendationReason(task, agronomist, score),
                                confidenceScore(score)
                        ));
                    });
        }

        return assignments;
    }

    private boolean hasCapacity(Agronomist agronomist, Map<UUID, Integer> assignmentCounts) {
        return assignmentCounts.getOrDefault(agronomist.getId(), 0) < agronomist.getMaxDailyVisit();
    }

    private static int score(
            FieldTask task,
            Agronomist agronomist,
            Map<UUID, Integer> assignmentCounts,
            LocalDate scheduleDate
    ) {
        int score = priorityScore(task.getPriority());
        if (!task.getDueDate().isAfter(scheduleDate)) {
            score += 30;
        } else if (task.getDueDate().isEqual(scheduleDate.plusDays(1))) {
            score += 20;
        }
        score += 20;
        score += 20;
        if (assignmentCounts.getOrDefault(agronomist.getId(), 0) < agronomist.getMaxDailyVisit()) {
            score += 10;
        }
        return score;
    }

    private static int priorityScore(TaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> 50;
            case HIGH -> 30;
            case MEDIUM -> 15;
            case LOW -> 5;
        };
    }

    private static int priorityRank(FieldTask task) {
        return priorityScore(task.getPriority());
    }

    private static String recommendationReason(FieldTask task, Agronomist agronomist, int score) {
        return "Recommended " + agronomist.getName()
                + " for " + task.getTaskType()
                + " based on priority " + task.getPriority()
                + ", due date " + task.getDueDate()
                + ", same district availability, and remaining daily capacity. Score: " + score;
    }

    private static BigDecimal confidenceScore(int score) {
        return BigDecimal.valueOf(Math.min(score, 100))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize scheduling agent payload", exception);
        }
    }

    private record RecommendedAssignment(
            FieldTask task,
            Agronomist agronomist,
            String reason,
            BigDecimal confidenceScore
    ) {
    }
}
