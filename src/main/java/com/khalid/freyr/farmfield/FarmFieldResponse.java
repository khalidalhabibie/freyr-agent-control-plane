package com.khalid.freyr.farmfield;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FarmFieldResponse(
        UUID id,
        UUID farmerId,
        String areaName,
        BigDecimal areaSize,
        CropStage cropStage,
        WaterStatus waterStatus,
        boolean pestReported,
        Instant lastVisitAt,
        Instant createdAt,
        Instant updatedAt
) {
}
