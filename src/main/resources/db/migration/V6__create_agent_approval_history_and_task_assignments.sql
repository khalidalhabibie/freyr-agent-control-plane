CREATE TABLE agent_approval_history (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES agent_proposals (id),
    action VARCHAR(50) NOT NULL,
    reviewed_by VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE task_assignments (
    id UUID PRIMARY KEY,
    field_task_id UUID NOT NULL REFERENCES field_tasks (id),
    agronomist_id UUID NOT NULL REFERENCES agronomists (id),
    proposal_id UUID NOT NULL REFERENCES agent_proposals (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE INDEX idx_agent_approval_history_proposal_id ON agent_approval_history (proposal_id);
CREATE INDEX idx_task_assignments_field_task_id ON task_assignments (field_task_id);
CREATE INDEX idx_task_assignments_agronomist_id ON task_assignments (agronomist_id);
CREATE INDEX idx_task_assignments_proposal_id ON task_assignments (proposal_id);
