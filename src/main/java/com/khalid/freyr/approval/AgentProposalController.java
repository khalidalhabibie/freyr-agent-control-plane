package com.khalid.freyr.approval;

import com.khalid.freyr.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent-proposals")
public class AgentProposalController {

    private final AgentProposalService agentProposalService;
    private final AgentApprovalService agentApprovalService;

    public AgentProposalController(
            AgentProposalService agentProposalService,
            AgentApprovalService agentApprovalService
    ) {
        this.agentProposalService = agentProposalService;
        this.agentApprovalService = agentApprovalService;
    }

    @GetMapping
    public ApiResponse<List<AgentProposalResponse>> getAgentProposals() {
        List<AgentProposalResponse> proposals = agentProposalService.getAgentProposals();
        return ApiResponse.success("Agent proposals retrieved", proposals);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentProposalResponse> getAgentProposal(@PathVariable UUID id) {
        AgentProposalResponse proposal = agentProposalService.getAgentProposal(id);
        return ApiResponse.success("Agent proposal retrieved", proposal);
    }

    @PostMapping("/{proposalId}/approve")
    public ApiResponse<AgentProposalResponse> approveAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ApproveAgentProposalRequest request
    ) {
        AgentProposalResponse proposal = agentApprovalService.approveProposal(proposalId, request);
        return ApiResponse.success("Agent proposal approved", proposal);
    }

    @PostMapping("/{proposalId}/reject")
    public ApiResponse<AgentProposalResponse> rejectAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody RejectAgentProposalRequest request
    ) {
        AgentProposalResponse proposal = agentApprovalService.rejectProposal(proposalId, request);
        return ApiResponse.success("Agent proposal rejected", proposal);
    }

    @PostMapping("/{proposalId}/override")
    public ApiResponse<AgentProposalResponse> overrideAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody OverrideAgentProposalRequest request
    ) {
        AgentProposalResponse proposal = agentApprovalService.overrideProposal(proposalId, request);
        return ApiResponse.success("Agent proposal overridden", proposal);
    }
}
