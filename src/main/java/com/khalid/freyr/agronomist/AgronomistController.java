package com.khalid.freyr.agronomist;

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
@RequestMapping("/api/v1/agronomists")
public class AgronomistController {

    private final AgronomistService agronomistService;

    public AgronomistController(AgronomistService agronomistService) {
        this.agronomistService = agronomistService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgronomistResponse> createAgronomist(@Valid @RequestBody CreateAgronomistRequest request) {
        AgronomistResponse agronomist = agronomistService.createAgronomist(request);
        return ApiResponse.success("Agronomist created", agronomist);
    }

    @GetMapping
    public ApiResponse<List<AgronomistResponse>> getAgronomists() {
        List<AgronomistResponse> agronomists = agronomistService.getAgronomists();
        return ApiResponse.success("Agronomists retrieved", agronomists);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgronomistResponse> getAgronomist(@PathVariable UUID id) {
        AgronomistResponse agronomist = agronomistService.getAgronomist(id);
        return ApiResponse.success("Agronomist retrieved", agronomist);
    }

    @PutMapping("/{id}")
    public ApiResponse<AgronomistResponse> updateAgronomist(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAgronomistRequest request
    ) {
        AgronomistResponse agronomist = agronomistService.updateAgronomist(id, request);
        return ApiResponse.success("Agronomist updated", agronomist);
    }
}
