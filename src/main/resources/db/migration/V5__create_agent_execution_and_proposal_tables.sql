CREATE TABLE agent_executions (
    id UUID PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL,
    execution_type VARCHAR(255) NOT NULL,
    input_payload TEXT NOT NULL,
    output_payload TEXT,
    status VARCHAR(50) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    prompt_version VARCHAR(255) NOT NULL,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_proposals (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES agent_executions (id),
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
    recommendation_reason VARCHAR(1000) NOT NULL,
    confidence_score NUMERIC(10, 4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_proposals_execution_id ON agent_proposals (execution_id);
CREATE INDEX idx_agent_proposals_schedule_date ON agent_proposals (schedule_date);
CREATE INDEX idx_agent_proposal_items_proposal_id ON agent_proposal_items (proposal_id);
CREATE INDEX idx_agent_proposal_items_field_task_id ON agent_proposal_items (field_task_id);
