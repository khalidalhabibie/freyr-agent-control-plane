package com.khalid.freyr.agent.execution;

import com.khalid.freyr.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent-executions")
public class AgentExecutionController {

    private final AgentExecutionService agentExecutionService;

    public AgentExecutionController(AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @GetMapping
    public ApiResponse<List<AgentExecutionResponse>> getAgentExecutions() {
        List<AgentExecutionResponse> executions = agentExecutionService.getExecutions();
        return ApiResponse.success("Agent executions retrieved", executions);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentExecutionResponse> getAgentExecution(@PathVariable UUID id) {
        AgentExecutionResponse execution = agentExecutionService.getExecution(id);
        return ApiResponse.success("Agent execution retrieved", execution);
    }
}
