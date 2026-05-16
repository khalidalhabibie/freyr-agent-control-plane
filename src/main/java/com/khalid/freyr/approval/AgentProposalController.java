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

    private final AgentApprovalService agentApprovalService;

    public AgentProposalController(AgentApprovalService agentApprovalService) {
        this.agentApprovalService = agentApprovalService;
    }

    @GetMapping
    public ApiResponse<List<AgentProposalSummaryResponse>> getAgentProposals() {
        List<AgentProposalSummaryResponse> proposals = agentApprovalService.getProposals();
        return ApiResponse.success("Agent proposals retrieved", proposals);
    }

    @GetMapping("/{proposalId}")
    public ApiResponse<AgentProposalDetailResponse> getAgentProposal(@PathVariable UUID proposalId) {
        AgentProposalDetailResponse proposal = agentApprovalService.getProposal(proposalId);
        return ApiResponse.success("Agent proposal retrieved", proposal);
    }

    @PostMapping("/{proposalId}/approve")
    public ApiResponse<AgentProposalDetailResponse> approveAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ApproveAgentProposalRequest request
    ) {
        AgentProposalDetailResponse proposal = agentApprovalService.approveProposal(proposalId, request);
        return ApiResponse.success("Agent proposal approved", proposal);
    }

    @PostMapping("/{proposalId}/reject")
    public ApiResponse<AgentProposalDetailResponse> rejectAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody RejectAgentProposalRequest request
    ) {
        AgentProposalDetailResponse proposal = agentApprovalService.rejectProposal(proposalId, request);
        return ApiResponse.success("Agent proposal rejected", proposal);
    }

    @PostMapping("/{proposalId}/override")
    public ApiResponse<AgentProposalDetailResponse> overrideAgentProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody OverrideAgentProposalRequest request
    ) {
        AgentProposalDetailResponse proposal = agentApprovalService.overrideProposal(proposalId, request);
        return ApiResponse.success("Agent proposal overridden", proposal);
    }
}
