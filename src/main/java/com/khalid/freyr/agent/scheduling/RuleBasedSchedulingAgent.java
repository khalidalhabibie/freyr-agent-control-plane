package com.khalid.freyr.agent.scheduling;

import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.agronomist.AvailabilityStatus;
import com.khalid.freyr.fieldtask.FieldTask;
import com.khalid.freyr.fieldtask.TaskPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RuleBasedSchedulingAgent implements SchedulingAgent {

    private static final int MAX_SCORE = 130;

    @Override
    public List<SchedulingRecommendation> recommendAssignments(
            List<FieldTask> tasks,
            List<Agronomist> agronomists,
            LocalDate scheduleDate,
            Map<UUID, Long> activeAssignmentCounts
    ) {
        List<FieldTask> orderedTasks = tasks.stream()
                .sorted(Comparator
                        .comparingInt((FieldTask task) -> priorityScore(task.getPriority())).reversed()
                        .thenComparing(FieldTask::getDueDate)
                        .thenComparing(FieldTask::getCreatedAt))
                .toList();
        List<Agronomist> orderedAgronomists = agronomists.stream()
                .sorted(Comparator.comparing(Agronomist::getName).thenComparing(Agronomist::getId))
                .toList();
        Map<UUID, Integer> proposedCounts = new HashMap<>();
        List<SchedulingRecommendation> recommendations = new ArrayList<>();

        for (FieldTask task : orderedTasks) {
            Agronomist selectedAgronomist = null;
            int selectedScore = -1;

            for (Agronomist agronomist : orderedAgronomists) {
                if (!hasRemainingCapacity(agronomist, activeAssignmentCounts, proposedCounts)) {
                    continue;
                }

                int score = score(task, agronomist, scheduleDate);
                if (score > selectedScore) {
                    selectedAgronomist = agronomist;
                    selectedScore = score;
                }
            }

            if (selectedAgronomist != null) {
                UUID agronomistId = selectedAgronomist.getId();
                proposedCounts.put(agronomistId, proposedCounts.getOrDefault(agronomistId, 0) + 1);
                recommendations.add(new SchedulingRecommendation(
                        task,
                        agronomistId,
                        selectedScore,
                        confidenceScore(selectedScore),
                        "Rule-based scheduling score " + selectedScore
                ));
            }
        }

        return recommendations;
    }

    private boolean hasRemainingCapacity(
            Agronomist agronomist,
            Map<UUID, Long> activeAssignmentCounts,
            Map<UUID, Integer> proposedCounts
    ) {
        UUID agronomistId = agronomist.getId();
        long activeAssignments = activeAssignmentCounts.getOrDefault(agronomistId, 0L);
        int proposedAssignments = proposedCounts.getOrDefault(agronomistId, 0);
        return activeAssignments + proposedAssignments < agronomist.getMaxDailyVisit();
    }

    private int score(FieldTask task, Agronomist agronomist, LocalDate scheduleDate) {
        int score = priorityScore(task.getPriority());

        if (!task.getDueDate().isAfter(scheduleDate)) {
            score += 30;
        } else if (task.getDueDate().isEqual(scheduleDate.plusDays(1))) {
            score += 20;
        }

        score += 20;

        if (agronomist.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
            score += 20;
        }

        score += 10;

        return score;
    }

    private int priorityScore(TaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> 50;
            case HIGH -> 30;
            case MEDIUM -> 15;
            case LOW -> 5;
        };
    }

    private BigDecimal confidenceScore(int score) {
        return BigDecimal.valueOf(score)
                .divide(BigDecimal.valueOf(MAX_SCORE), 4, RoundingMode.HALF_UP);
    }
}
