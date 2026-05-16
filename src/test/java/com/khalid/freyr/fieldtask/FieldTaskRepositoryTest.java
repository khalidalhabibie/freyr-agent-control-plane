package com.khalid.freyr.fieldtask;

import com.khalid.freyr.farmer.Farmer;
import com.khalid.freyr.farmfield.CropStage;
import com.khalid.freyr.farmfield.FarmField;
import com.khalid.freyr.farmfield.WaterStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:field_task_repository_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class FieldTaskRepositoryTest {

    @Autowired
    private FieldTaskRepository fieldTaskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findEligibleUnassignedTasksIncludesOverdueTodayAndTomorrowOnlyWhenCreated() {
        LocalDate scheduleDate = LocalDate.of(2026, 5, 9);
        FarmField farmField = farmField("Aceh Besar");

        FieldTask overdue = persistTask(farmField, TaskPriority.HIGH, TaskStatus.CREATED, scheduleDate.minusDays(1));
        FieldTask today = persistTask(farmField, TaskPriority.MEDIUM, TaskStatus.CREATED, scheduleDate);
        FieldTask tomorrow = persistTask(farmField, TaskPriority.LOW, TaskStatus.CREATED, scheduleDate.plusDays(1));
        FieldTask afterTomorrow = persistTask(
                farmField,
                TaskPriority.CRITICAL,
                TaskStatus.CREATED,
                scheduleDate.plusDays(2)
        );
        FieldTask proposed = persistTask(farmField, TaskPriority.CRITICAL, TaskStatus.PROPOSED, scheduleDate);
        FieldTask assigned = persistTask(farmField, TaskPriority.CRITICAL, TaskStatus.ASSIGNED, scheduleDate);
        FieldTask completed = persistTask(farmField, TaskPriority.CRITICAL, TaskStatus.COMPLETED, scheduleDate);
        FieldTask cancelled = persistTask(farmField, TaskPriority.CRITICAL, TaskStatus.CANCELLED, scheduleDate);

        entityManager.flush();
        entityManager.clear();

        List<FieldTask> tasks = fieldTaskRepository.findEligibleUnassignedTasksByDistrictAndDueDateWindow(
                "Aceh Besar",
                scheduleDate.plusDays(1)
        );

        assertThat(tasks)
                .extracting(FieldTask::getId)
                .contains(overdue.getId(), today.getId(), tomorrow.getId())
                .doesNotContain(
                        afterTomorrow.getId(),
                        proposed.getId(),
                        assigned.getId(),
                        completed.getId(),
                        cancelled.getId()
                );
    }

    private FarmField farmField(String district) {
        Farmer farmer = new Farmer(
                "Pak Budi",
                "08123456789",
                "Lamteh",
                district
        );
        entityManager.persist(farmer);

        FarmField farmField = new FarmField(
                farmer.getId(),
                "North Block",
                new BigDecimal("2.50"),
                CropStage.VEGETATIVE,
                WaterStatus.WET,
                false,
                null
        );
        entityManager.persist(farmField);
        return farmField;
    }

    private FieldTask persistTask(
            FarmField farmField,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate
    ) {
        FieldTask task = new FieldTask(
                farmField.getId(),
                TaskType.WATER_LEVEL_CHECK,
                priority,
                status,
                dueDate,
                null,
                null
        );
        entityManager.persist(task);
        return task;
    }
}
