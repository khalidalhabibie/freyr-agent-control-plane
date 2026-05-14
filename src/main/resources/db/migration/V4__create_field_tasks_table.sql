CREATE TABLE field_tasks (
    id UUID PRIMARY KEY,
    farm_field_id UUID NOT NULL REFERENCES farm_fields (id),
    task_type VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    due_date DATE NOT NULL,
    assigned_agronomist_id UUID REFERENCES agronomists (id),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_field_tasks_farm_field_id ON field_tasks (farm_field_id);
CREATE INDEX idx_field_tasks_assigned_agronomist_id ON field_tasks (assigned_agronomist_id);
CREATE INDEX idx_field_tasks_unassigned_due_date ON field_tasks (due_date)
    WHERE assigned_agronomist_id IS NULL;
