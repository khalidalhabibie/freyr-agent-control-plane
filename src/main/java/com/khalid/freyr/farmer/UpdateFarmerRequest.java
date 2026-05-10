package com.khalid.freyr.farmer;

import jakarta.validation.constraints.NotBlank;

public record UpdateFarmerRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "phoneNumber is required")
        String phoneNumber,

        @NotBlank(message = "village is required")
        String village,

        @NotBlank(message = "district is required")
        String district
) {
}
