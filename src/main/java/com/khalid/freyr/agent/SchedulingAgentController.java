package com.khalid.freyr.agent;

import com.khalid.freyr.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents/scheduling")
public class SchedulingAgentController {

    private final SchedulingAgent schedulingAgent;

    public SchedulingAgentController(SchedulingAgent schedulingAgent) {
        this.schedulingAgent = schedulingAgent;
    }

    @PostMapping("/run")
    public ApiResponse<RunSchedulingAgentResponse> runSchedulingAgent(
            @Valid @RequestBody RunSchedulingAgentRequest request
    ) {
        RunSchedulingAgentResponse response = schedulingAgent.run(request);
        return ApiResponse.success(response.message(), response);
    }
}
