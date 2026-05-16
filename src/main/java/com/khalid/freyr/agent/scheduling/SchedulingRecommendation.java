package com.khalid.freyr.agent.scheduling;

import com.khalid.freyr.fieldtask.FieldTask;

import java.math.BigDecimal;
import java.util.UUID;

public record SchedulingRecommendation(
        FieldTask fieldTask,
        UUID agronomistId,
        int score,
        BigDecimal confidenceScore,
        String reason
) {
}
