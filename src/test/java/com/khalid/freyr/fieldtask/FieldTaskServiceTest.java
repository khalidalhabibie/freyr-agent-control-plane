package com.khalid.freyr.fieldtask;

import com.khalid.freyr.farmfield.FarmFieldRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldTaskServiceTest {

    @Mock
    private FieldTaskRepository fieldTaskRepository;

    @Mock
    private FarmFieldRepository farmFieldRepository;

    @InjectMocks
    private FieldTaskService fieldTaskService;

    @Test
    void createFieldTaskSavesAndReturnsTaskWhenFarmFieldExists() {
        UUID farmFieldId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.of(2026, 5, 20);
        CreateFieldTaskRequest request = new CreateFieldTaskRequest(
                farmFieldId,
                TaskType.PEST_INSPECTION,
                TaskPriority.HIGH,
                TaskStatus.CREATED,
                dueDate,
                agronomistId,
                null
        );

        when(farmFieldRepository.existsById(farmFieldId)).thenReturn(true);
        when(fieldTaskRepository.save(any(FieldTask.class))).thenAnswer(invocation -> {
            FieldTask fieldTask = invocation.getArgument(0);
            fieldTask.prePersist();
            return fieldTask;
        });

        FieldTaskResponse response = fieldTaskService.createFieldTask(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.farmFieldId()).isEqualTo(farmFieldId);
        assertThat(response.taskType()).isEqualTo(TaskType.PEST_INSPECTION);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(TaskStatus.CREATED);
        assertThat(response.dueDate()).isEqualTo(dueDate);
        assertThat(response.assignedAgronomistId()).isEqualTo(agronomistId);
        assertThat(response.completedAt()).isNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(fieldTaskRepository).save(any(FieldTask.class));
    }

    @Test
    void createFieldTaskThrowsWhenFarmFieldDoesNotExist() {
        UUID farmFieldId = UUID.randomUUID();
        CreateFieldTaskRequest request = new CreateFieldTaskRequest(
                farmFieldId,
                TaskType.WATER_LEVEL_CHECK,
                TaskPriority.MEDIUM,
                TaskStatus.CREATED,
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        when(farmFieldRepository.existsById(farmFieldId)).thenReturn(false);

        assertThatThrownBy(() -> fieldTaskService.createFieldTask(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farm field not found");

        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
    }

    @Test
    void getFieldTasksReturnsAllTasks() {
        FieldTask fieldTask = persistedFieldTask(
                UUID.randomUUID(),
                TaskType.FERTILIZER_GUIDANCE,
                TaskPriority.LOW,
                TaskStatus.PROPOSED,
                LocalDate.of(2026, 5, 21),
                null,
                null
        );

        when(fieldTaskRepository.findAll()).thenReturn(List.of(fieldTask));

        List<FieldTaskResponse> responses = fieldTaskService.getFieldTasks();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().taskType()).isEqualTo(TaskType.FERTILIZER_GUIDANCE);
        assertThat(responses.getFirst().status()).isEqualTo(TaskStatus.PROPOSED);
    }

    @Test
    void getFieldTaskReturnsTaskWhenFound() {
        FieldTask fieldTask = persistedFieldTask(
                UUID.randomUUID(),
                TaskType.HARVEST_READINESS_CHECK,
                TaskPriority.CRITICAL,
                TaskStatus.ASSIGNED,
                LocalDate.of(2026, 5, 22),
                UUID.randomUUID(),
                null
        );
        UUID id = fieldTask.getId();

        when(fieldTaskRepository.findById(id)).thenReturn(Optional.of(fieldTask));

        FieldTaskResponse response = fieldTaskService.getFieldTask(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.priority()).isEqualTo(TaskPriority.CRITICAL);
        assertThat(response.status()).isEqualTo(TaskStatus.ASSIGNED);
    }

    @Test
    void getFieldTaskThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(fieldTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTaskService.getFieldTask(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Field task not found");
    }

    @Test
    void updateFieldTaskUpdatesAndReturnsTaskWhenFarmFieldExists() {
        UUID originalFarmFieldId = UUID.randomUUID();
        UUID updatedFarmFieldId = UUID.randomUUID();
        UUID agronomistId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-05-23T10:15:30Z");
        FieldTask fieldTask = persistedFieldTask(
                originalFarmFieldId,
                TaskType.WATER_LEVEL_CHECK,
                TaskPriority.MEDIUM,
                TaskStatus.CREATED,
                LocalDate.of(2026, 5, 20),
                null,
                null
        );
        UUID id = fieldTask.getId();
        UpdateFieldTaskRequest request = new UpdateFieldTaskRequest(
                updatedFarmFieldId,
                TaskType.CULTIVATION_LOG_VERIFICATION,
                TaskPriority.HIGH,
                TaskStatus.COMPLETED,
                LocalDate.of(2026, 5, 23),
                agronomistId,
                completedAt
        );

        when(fieldTaskRepository.findById(id)).thenReturn(Optional.of(fieldTask));
        when(farmFieldRepository.existsById(updatedFarmFieldId)).thenReturn(true);
        when(fieldTaskRepository.save(fieldTask)).thenAnswer(invocation -> {
            FieldTask savedFieldTask = invocation.getArgument(0);
            savedFieldTask.preUpdate();
            return savedFieldTask;
        });

        FieldTaskResponse response = fieldTaskService.updateFieldTask(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.farmFieldId()).isEqualTo(updatedFarmFieldId);
        assertThat(response.taskType()).isEqualTo(TaskType.CULTIVATION_LOG_VERIFICATION);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(response.assignedAgronomistId()).isEqualTo(agronomistId);
        assertThat(response.completedAt()).isEqualTo(completedAt);
        verify(fieldTaskRepository).save(fieldTask);
    }

    @Test
    void updateFieldTaskThrowsWhenTaskNotFound() {
        UUID id = UUID.randomUUID();
        UpdateFieldTaskRequest request = updateRequest(UUID.randomUUID());

        when(fieldTaskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTaskService.updateFieldTask(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Field task not found");

        verify(farmFieldRepository, never()).existsById(any(UUID.class));
        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
    }

    @Test
    void updateFieldTaskThrowsWhenFarmFieldDoesNotExist() {
        UUID farmFieldId = UUID.randomUUID();
        FieldTask fieldTask = persistedFieldTask(
                UUID.randomUUID(),
                TaskType.WATER_LEVEL_CHECK,
                TaskPriority.MEDIUM,
                TaskStatus.CREATED,
                LocalDate.of(2026, 5, 20),
                null,
                null
        );
        UUID id = fieldTask.getId();
        UpdateFieldTaskRequest request = updateRequest(farmFieldId);

        when(fieldTaskRepository.findById(id)).thenReturn(Optional.of(fieldTask));
        when(farmFieldRepository.existsById(farmFieldId)).thenReturn(false);

        assertThatThrownBy(() -> fieldTaskService.updateFieldTask(id, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Farm field not found");

        verify(fieldTaskRepository, never()).save(any(FieldTask.class));
    }

    @Test
    void getUnassignedTasksByDistrictAndDueDateReturnsMatchingTasks() {
        String district = "Subang";
        LocalDate dueDate = LocalDate.of(2026, 5, 24);
        FieldTask fieldTask = persistedFieldTask(
                UUID.randomUUID(),
                TaskType.PEST_INSPECTION,
                TaskPriority.CRITICAL,
                TaskStatus.CREATED,
                dueDate,
                null,
                null
        );

        when(fieldTaskRepository.findEligibleUnassignedTasksByDistrictAndDueDateWindow(district, dueDate.plusDays(1)))
                .thenReturn(List.of(fieldTask));

        List<FieldTaskResponse> responses = fieldTaskService.getUnassignedTasksByDistrictAndDueDate(district, dueDate);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().taskType()).isEqualTo(TaskType.PEST_INSPECTION);
        assertThat(responses.getFirst().assignedAgronomistId()).isNull();
        verify(fieldTaskRepository).findEligibleUnassignedTasksByDistrictAndDueDateWindow(district, dueDate.plusDays(1));
    }

    private UpdateFieldTaskRequest updateRequest(UUID farmFieldId) {
        return new UpdateFieldTaskRequest(
                farmFieldId,
                TaskType.CULTIVATION_LOG_VERIFICATION,
                TaskPriority.HIGH,
                TaskStatus.COMPLETED,
                LocalDate.of(2026, 5, 23),
                UUID.randomUUID(),
                Instant.parse("2026-05-23T10:15:30Z")
        );
    }

    private FieldTask persistedFieldTask(
            UUID farmFieldId,
            TaskType taskType,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            UUID assignedAgronomistId,
            Instant completedAt
    ) {
        FieldTask fieldTask = new FieldTask(
                farmFieldId,
                taskType,
                priority,
                status,
                dueDate,
                assignedAgronomistId,
                completedAt
        );
        fieldTask.prePersist();
        return fieldTask;
    }
}
