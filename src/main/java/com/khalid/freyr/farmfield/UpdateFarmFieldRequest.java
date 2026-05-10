package com.khalid.freyr.farmfield;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateFarmFieldRequest(
        @NotNull(message = "farmerId is required")
        UUID farmerId,

        @NotBlank(message = "areaName is required")
        String areaName,

        @NotNull(message = "areaSize is required")
        @DecimalMin(value = "0.01", message = "areaSize must be greater than zero")
        BigDecimal areaSize,

        @NotNull(message = "cropStage is required")
        CropStage cropStage,

        @NotNull(message = "waterStatus is required")
        WaterStatus waterStatus,

        @NotNull(message = "pestReported is required")
        Boolean pestReported,

        Instant lastVisitAt
) {
}
