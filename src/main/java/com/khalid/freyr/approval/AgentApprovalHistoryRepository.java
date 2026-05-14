package com.khalid.freyr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentApprovalHistoryRepository extends JpaRepository<AgentApprovalHistory, UUID> {
}
