package com.khalid.freyr.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RunSchedulingAgentRequest(
        @NotBlank(message = "district is required")
        String district,

        @NotNull(message = "scheduleDate is required")
        LocalDate scheduleDate
) {
}
