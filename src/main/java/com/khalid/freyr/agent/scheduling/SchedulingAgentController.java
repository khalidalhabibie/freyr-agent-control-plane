package com.khalid.freyr.agent.scheduling;

import com.khalid.freyr.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents/scheduling")
public class SchedulingAgentController {

    private final SchedulingAgentService schedulingAgentService;

    public SchedulingAgentController(SchedulingAgentService schedulingAgentService) {
        this.schedulingAgentService = schedulingAgentService;
    }

    @PostMapping("/run")
    public ApiResponse<RunSchedulingAgentResponse> runSchedulingAgent(
            @Valid @RequestBody RunSchedulingAgentRequest request
    ) {
        RunSchedulingAgentResponse response = schedulingAgentService.runSchedulingAgent(request);
        return ApiResponse.success("Scheduling proposal generated", response);
    }
}
