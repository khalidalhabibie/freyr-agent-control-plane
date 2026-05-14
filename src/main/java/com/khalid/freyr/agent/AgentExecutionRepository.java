package com.khalid.freyr.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentExecutionRepository extends JpaRepository<AgentExecution, UUID> {
}
