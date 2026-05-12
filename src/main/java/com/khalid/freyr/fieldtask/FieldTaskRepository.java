package com.khalid.freyr.fieldtask;

import com.khalid.freyr.farmer.Farmer;
import com.khalid.freyr.farmfield.FarmField;
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
              and task.dueDate = :dueDate
            order by task.priority desc, task.createdAt asc
            """)
    List<FieldTask> findUnassignedTasksByDistrictAndDueDate(
            @Param("district") String district,
            @Param("dueDate") LocalDate dueDate
    );
}
