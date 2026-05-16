package com.khalid.freyr.fieldtask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FieldTaskRepository extends JpaRepository<FieldTask, UUID> {

    @Query("""
            select task
            from FieldTask task
            join FarmField field on field.id = task.farmFieldId
            join Farmer farmer on farmer.id = field.farmerId
            where task.assignedAgronomistId is null
              and farmer.district = :district
              and task.status = com.khalid.freyr.fieldtask.TaskStatus.CREATED
              and task.dueDate <= :dueDateCutoff
            order by
              case task.priority
                when com.khalid.freyr.fieldtask.TaskPriority.CRITICAL then 4
                when com.khalid.freyr.fieldtask.TaskPriority.HIGH then 3
                when com.khalid.freyr.fieldtask.TaskPriority.MEDIUM then 2
                when com.khalid.freyr.fieldtask.TaskPriority.LOW then 1
              end desc,
              task.dueDate asc,
              task.createdAt asc
            """)
    List<FieldTask> findEligibleUnassignedTasksByDistrictAndDueDateWindow(
            @Param("district") String district,
            @Param("dueDateCutoff") LocalDate dueDateCutoff
    );
}
