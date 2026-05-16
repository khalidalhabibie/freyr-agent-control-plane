package com.khalid.freyr.agent.scheduling;

import com.khalid.freyr.agronomist.Agronomist;
import com.khalid.freyr.fieldtask.FieldTask;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SchedulingAgent {

    List<SchedulingRecommendation> recommendAssignments(
            List<FieldTask> tasks,
            List<Agronomist> agronomists,
            LocalDate scheduleDate,
            Map<UUID, Long> activeAssignmentCounts
    );
}
