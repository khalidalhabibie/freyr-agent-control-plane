package com.khalid.freyr.farmer;

import com.khalid.freyr.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farmers")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmerResponse> createFarmer(@Valid @RequestBody CreateFarmerRequest request) {
        FarmerResponse farmer = farmerService.createFarmer(request);
        return ApiResponse.success("Farmer created", farmer);
    }

    @GetMapping
    public ApiResponse<List<FarmerResponse>> getFarmers() {
        List<FarmerResponse> farmers = farmerService.getFarmers();
        return ApiResponse.success("Farmers retrieved", farmers);
    }

    @GetMapping("/{id}")
    public ApiResponse<FarmerResponse> getFarmer(@PathVariable UUID id) {
        FarmerResponse farmer = farmerService.getFarmer(id);
        return ApiResponse.success("Farmer retrieved", farmer);
    }

    @PutMapping("/{id}")
    public ApiResponse<FarmerResponse> updateFarmer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFarmerRequest request
    ) {
        FarmerResponse farmer = farmerService.updateFarmer(id, request);
        return ApiResponse.success("Farmer updated", farmer);
    }
}
