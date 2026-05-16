package com.khalid.freyr.agent.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khalid.freyr.agent.execution.AgentExecutionResponse;
import com.khalid.freyr.agent.execution.AgentExecutionService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SchedulingAgentService {

    private static final String AGENT_NAME = "rule-based-scheduling-agent";
    private static final String EXECUTION_TYPE = "SCHEDULING";
    private static final String MODEL_NAME = "rules-v1";
    private static final String PROMPT_VERSION = "rule-based-v1";

    private final AgentExecutionService agentExecutionService;
    private final SchedulingProposalWriter schedulingProposalWriter;
    private final ObjectMapper objectMapper;

    public SchedulingAgentService(
            AgentExecutionService agentExecutionService,
            SchedulingProposalWriter schedulingProposalWriter,
            ObjectMapper objectMapper
    ) {
        this.agentExecutionService = agentExecutionService;
        this.schedulingProposalWriter = schedulingProposalWriter;
        this.objectMapper = objectMapper;
    }

    public RunSchedulingAgentResponse runSchedulingAgent(RunSchedulingAgentRequest request) {
        AgentExecutionResponse execution = agentExecutionService.createRunningExecution(
                AGENT_NAME,
                EXECUTION_TYPE,
                inputPayload(request),
                MODEL_NAME,
                PROMPT_VERSION
        );

        try {
            return schedulingProposalWriter.generateProposal(request, execution);
        } catch (SchedulingAgentException exception) {
            agentExecutionService.markExecutionFailed(execution.id(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            agentExecutionService.markExecutionFailed(execution.id(), "Scheduling agent failed unexpectedly");
            throw exception;
        }
    }

    private String inputPayload(RunSchedulingAgentRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("district", request.district());
        payload.put("scheduleDate", request.scheduleDate());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize scheduling input payload", exception);
        }
    }
}
