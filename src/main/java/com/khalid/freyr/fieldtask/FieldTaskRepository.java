package com.khalid.freyr.fieldtask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FieldTaskRepository extends JpaRepository<FieldTask, UUID> {

    @Query(
            value = """
                    SELECT ft.*
                    FROM field_tasks ft
                    JOIN farm_fields ff ON ff.id = ft.farm_field_id
                    JOIN farmers f ON f.id = ff.farmer_id
                    WHERE ft.assigned_agronomist_id IS NULL
                      AND f.district = :district
                      AND ft.due_date = :dueDate
                    """,
            nativeQuery = true
    )
    List<FieldTask> findUnassignedTasksByDistrictAndDueDate(
            @Param("district") String district,
            @Param("dueDate") LocalDate dueDate
    );
}
