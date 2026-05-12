package com.khalid.freyr.agronomist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAgronomistRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "phoneNumber is required")
        String phoneNumber,

        @NotBlank(message = "assignedDistrict is required")
        String assignedDistrict,

        @NotNull(message = "maxDailyVisit is required")
        @Min(value = 1, message = "maxDailyVisit must be at least 1")
        Integer maxDailyVisit,

        @NotNull(message = "availabilityStatus is required")
        AvailabilityStatus availabilityStatus
) {
}
