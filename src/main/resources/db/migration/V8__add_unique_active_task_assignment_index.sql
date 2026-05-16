CREATE UNIQUE INDEX ux_task_assignments_active_field_task
    ON task_assignments (field_task_id)
    WHERE status = 'ACTIVE';
