package com.khalid.freyr.farmfield;

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
@RequestMapping("/api/v1/fields")
public class FarmFieldController {

    private final FarmFieldService farmFieldService;

    public FarmFieldController(FarmFieldService farmFieldService) {
        this.farmFieldService = farmFieldService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmFieldResponse> createFarmField(@Valid @RequestBody CreateFarmFieldRequest request) {
        FarmFieldResponse farmField = farmFieldService.createFarmField(request);
        return ApiResponse.success("Farm field created", farmField);
    }

    @GetMapping
    public ApiResponse<List<FarmFieldResponse>> getFarmFields() {
        List<FarmFieldResponse> farmFields = farmFieldService.getFarmFields();
        return ApiResponse.success("Farm fields retrieved", farmFields);
    }

    @GetMapping("/{id}")
    public ApiResponse<FarmFieldResponse> getFarmField(@PathVariable UUID id) {
        FarmFieldResponse farmField = farmFieldService.getFarmField(id);
        return ApiResponse.success("Farm field retrieved", farmField);
    }

    @PutMapping("/{id}")
    public ApiResponse<FarmFieldResponse> updateFarmField(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFarmFieldRequest request
    ) {
        FarmFieldResponse farmField = farmFieldService.updateFarmField(id, request);
        return ApiResponse.success("Farm field updated", farmField);
    }
}
