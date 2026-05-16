DO $$
BEGIN
    IF to_regclass('public.agent_executions') IS NOT NULL THEN
        ALTER TABLE agent_executions
            ADD CONSTRAINT chk_agent_executions_status
            CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'));
    END IF;

    IF to_regclass('public.agent_execution') IS NOT NULL THEN
        ALTER TABLE agent_execution
            ADD CONSTRAINT chk_agent_execution_status
            CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'));
    END IF;
END $$;

ALTER TABLE agent_proposals
    ADD CONSTRAINT chk_agent_proposals_status
    CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'OVERRIDDEN', 'EXECUTED'));

ALTER TABLE agent_proposal_items
    ADD CONSTRAINT chk_agent_proposal_items_status
    CHECK (status IN ('PROPOSED', 'APPROVED', 'REJECTED', 'OVERRIDDEN', 'ASSIGNED'));

ALTER TABLE agent_proposal_items
    ADD CONSTRAINT chk_agent_proposal_items_confidence_score_range
    CHECK (confidence_score BETWEEN 0 AND 1);

ALTER TABLE agent_approval_histories
    ADD CONSTRAINT chk_agent_approval_histories_action
    CHECK (action IN ('APPROVED', 'REJECTED', 'OVERRIDDEN'));

ALTER TABLE task_assignments
    ADD CONSTRAINT chk_task_assignments_status
    CHECK (status IN ('ACTIVE', 'CANCELLED'));
