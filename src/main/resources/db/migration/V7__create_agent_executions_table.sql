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
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_agent_executions_status
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_agent_executions_status ON agent_executions (status);
CREATE INDEX idx_agent_executions_agent_name ON agent_executions (agent_name);
CREATE INDEX idx_agent_executions_created_at ON agent_executions (created_at);
