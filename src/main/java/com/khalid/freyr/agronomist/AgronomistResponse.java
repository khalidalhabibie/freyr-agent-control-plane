package com.khalid.freyr.agronomist;

import java.time.Instant;
import java.util.UUID;

public record AgronomistResponse(
        UUID id,
        String name,
        String phoneNumber,
        String assignedDistrict,
        Integer maxDailyVisit,
        AvailabilityStatus availabilityStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
