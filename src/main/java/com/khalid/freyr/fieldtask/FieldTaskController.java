package com.khalid.freyr.fieldtask;

import com.khalid.freyr.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/field-tasks")
public class FieldTaskController {

    private final FieldTaskService fieldTaskService;

    public FieldTaskController(FieldTaskService fieldTaskService) {
        this.fieldTaskService = fieldTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FieldTaskResponse> createFieldTask(@Valid @RequestBody CreateFieldTaskRequest request) {
        FieldTaskResponse fieldTask = fieldTaskService.createFieldTask(request);
        return ApiResponse.success("Field task created", fieldTask);
    }

    @GetMapping
    public ApiResponse<List<FieldTaskResponse>> getFieldTasks(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) {
        if (district != null || dueDate != null) {
            if (district == null || dueDate == null) {
                throw new IllegalArgumentException("district and dueDate must be provided together");
            }
            List<FieldTaskResponse> fieldTasks = fieldTaskService.getUnassignedTasksByDistrictAndDueDate(
                    district,
                    dueDate
            );
            return ApiResponse.success("Unassigned field tasks retrieved", fieldTasks);
        }

        List<FieldTaskResponse> fieldTasks = fieldTaskService.getFieldTasks();
        return ApiResponse.success("Field tasks retrieved", fieldTasks);
    }

    @GetMapping("/{id}")
    public ApiResponse<FieldTaskResponse> getFieldTask(@PathVariable UUID id) {
        FieldTaskResponse fieldTask = fieldTaskService.getFieldTask(id);
        return ApiResponse.success("Field task retrieved", fieldTask);
    }

    @PutMapping("/{id}")
    public ApiResponse<FieldTaskResponse> updateFieldTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFieldTaskRequest request
    ) {
        FieldTaskResponse fieldTask = fieldTaskService.updateFieldTask(id, request);
        return ApiResponse.success("Field task updated", fieldTask);
    }
}
