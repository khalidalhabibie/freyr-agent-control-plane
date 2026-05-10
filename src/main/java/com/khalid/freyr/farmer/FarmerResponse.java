package com.khalid.freyr.farmer;

import java.time.Instant;
import java.util.UUID;

public record FarmerResponse(
        UUID id,
        String name,
        String phoneNumber,
        String village,
        String district,
        Instant createdAt,
        Instant updatedAt
) {
}
