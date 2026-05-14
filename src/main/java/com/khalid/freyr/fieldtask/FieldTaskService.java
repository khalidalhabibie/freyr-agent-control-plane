package com.khalid.freyr.fieldtask;

import com.khalid.freyr.agronomist.AgronomistRepository;
import com.khalid.freyr.farmfield.FarmFieldRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FieldTaskService {

    private final FieldTaskRepository fieldTaskRepository;
    private final FarmFieldRepository farmFieldRepository;
    private final AgronomistRepository agronomistRepository;

    public FieldTaskService(
            FieldTaskRepository fieldTaskRepository,
            FarmFieldRepository farmFieldRepository,
            AgronomistRepository agronomistRepository
    ) {
        this.fieldTaskRepository = fieldTaskRepository;
        this.farmFieldRepository = farmFieldRepository;
        this.agronomistRepository = agronomistRepository;
    }

    @Transactional
    public FieldTaskResponse createFieldTask(CreateFieldTaskRequest request) {
        validateFarmFieldExists(request.farmFieldId());
        validateAssignedAgronomistExists(request.assignedAgronomistId());

        FieldTask fieldTask = new FieldTask(
                request.farmFieldId(),
                request.taskType(),
                request.priority(),
                request.status(),
                request.dueDate(),
                request.assignedAgronomistId(),
                request.completedAt()
        );

        FieldTask savedFieldTask = fieldTaskRepository.save(fieldTask);
        return toResponse(savedFieldTask);
    }

    @Transactional(readOnly = true)
    public List<FieldTaskResponse> getFieldTasks() {
        return fieldTaskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldTaskResponse getFieldTask(UUID id) {
        return fieldTaskRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Field task not found"));
    }

    @Transactional(readOnly = true)
    public List<FieldTaskResponse> getUnassignedTasksByDistrictAndDueDate(String district, LocalDate dueDate) {
        return fieldTaskRepository.findUnassignedTasksByDistrictAndDueDate(district, dueDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FieldTaskResponse updateFieldTask(UUID id, UpdateFieldTaskRequest request) {
        FieldTask fieldTask = fieldTaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Field task not found"));

        validateFarmFieldExists(request.farmFieldId());
        validateAssignedAgronomistExists(request.assignedAgronomistId());

        fieldTask.update(
                request.farmFieldId(),
                request.taskType(),
                request.priority(),
                request.status(),
                request.dueDate(),
                request.assignedAgronomistId(),
                request.completedAt()
        );

        FieldTask savedFieldTask = fieldTaskRepository.save(fieldTask);
        return toResponse(savedFieldTask);
    }

    private void validateFarmFieldExists(UUID farmFieldId) {
        if (!farmFieldRepository.existsById(farmFieldId)) {
            throw new EntityNotFoundException("Farm field not found");
        }
    }

    private void validateAssignedAgronomistExists(UUID assignedAgronomistId) {
        if (assignedAgronomistId != null && !agronomistRepository.existsById(assignedAgronomistId)) {
            throw new EntityNotFoundException("Agronomist not found");
        }
    }

    private FieldTaskResponse toResponse(FieldTask fieldTask) {
        return new FieldTaskResponse(
                fieldTask.getId(),
                fieldTask.getFarmFieldId(),
                fieldTask.getTaskType(),
                fieldTask.getPriority(),
                fieldTask.getStatus(),
                fieldTask.getDueDate(),
                fieldTask.getAssignedAgronomistId(),
                fieldTask.getCompletedAt(),
                fieldTask.getCreatedAt(),
                fieldTask.getUpdatedAt()
        );
    }
}
