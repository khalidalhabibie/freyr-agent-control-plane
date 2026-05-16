CREATE TABLE agent_proposals (
    id UUID PRIMARY KEY,
    execution_id UUID,
    proposal_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    district VARCHAR(255) NOT NULL,
    schedule_date DATE NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_proposal_items (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES agent_proposals (id),
    field_task_id UUID NOT NULL REFERENCES field_tasks (id),
    recommended_agronomist_id UUID NOT NULL REFERENCES agronomists (id),
    original_agronomist_id UUID REFERENCES agronomists (id),
    overridden_agronomist_id UUID REFERENCES agronomists (id),
    override_reason VARCHAR(1000),
    overridden_at TIMESTAMPTZ,
    recommendation_reason VARCHAR(1000) NOT NULL,
    confidence_score NUMERIC(10, 4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_approval_histories (
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

CREATE INDEX idx_agent_proposals_schedule_date ON agent_proposals (schedule_date);
CREATE INDEX idx_agent_proposal_items_proposal_id ON agent_proposal_items (proposal_id);
CREATE INDEX idx_agent_approval_histories_proposal_id ON agent_approval_histories (proposal_id);
CREATE INDEX idx_task_assignments_field_task_id ON task_assignments (field_task_id);
CREATE INDEX idx_task_assignments_agronomist_id ON task_assignments (agronomist_id);
CREATE INDEX idx_task_assignments_proposal_id ON task_assignments (proposal_id);
