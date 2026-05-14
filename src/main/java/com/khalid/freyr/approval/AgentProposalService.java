package com.khalid.freyr.approval;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentProposalService {

    private final AgentProposalRepository agentProposalRepository;
    private final AgentProposalItemRepository agentProposalItemRepository;

    public AgentProposalService(
            AgentProposalRepository agentProposalRepository,
            AgentProposalItemRepository agentProposalItemRepository
    ) {
        this.agentProposalRepository = agentProposalRepository;
        this.agentProposalItemRepository = agentProposalItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentProposalResponse> getAgentProposals() {
        return agentProposalRepository.findAll()
                .stream()
                .map(proposal -> toResponse(proposal, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentProposalResponse getAgentProposal(UUID id) {
        AgentProposal proposal = agentProposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent proposal not found"));
        List<AgentProposalItemResponse> items = agentProposalItemRepository.findByProposalId(id)
                .stream()
                .map(this::toItemResponse)
                .toList();

        return toResponse(proposal, items);
    }

    private AgentProposalResponse toResponse(AgentProposal proposal, List<AgentProposalItemResponse> items) {
        return new AgentProposalResponse(
                proposal.getId(),
                proposal.getExecutionId(),
                proposal.getProposalType(),
                proposal.getStatus(),
                proposal.getDistrict(),
                proposal.getScheduleDate(),
                proposal.getSummary(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt(),
                items
        );
    }

    private AgentProposalItemResponse toItemResponse(AgentProposalItem item) {
        return new AgentProposalItemResponse(
                item.getId(),
                item.getProposalId(),
                item.getFieldTaskId(),
                item.getRecommendedAgronomistId(),
                item.getRecommendationReason(),
                item.getConfidenceScore(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
