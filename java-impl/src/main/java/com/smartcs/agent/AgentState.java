package com.smartcs.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent全局状态 —— 在Supervisor编排流程中流转的上下文对象。
 * 所有子Agent读写同一个State实例，实现状态共享。
 */
public class AgentState {
    private String userId;
    private String sessionId;
    private String userMessage;
    private String intent;
    private Map<String, Object> subResults = new HashMap<>();
    private boolean compliancePassed = true;
    private String finalResponse;
    private String currentAgent;
    private int retryCount = 0;

    public AgentState() {}

    public AgentState(String userId, String sessionId, String userMessage) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Map<String, Object> getSubResults() { return subResults; }
    public void setSubResults(Map<String, Object> subResults) { this.subResults = subResults; }

    public boolean isCompliancePassed() { return compliancePassed; }
    public void setCompliancePassed(boolean compliancePassed) { this.compliancePassed = compliancePassed; }

    public String getFinalResponse() { return finalResponse; }
    public void setFinalResponse(String finalResponse) { this.finalResponse = finalResponse; }

    public String getCurrentAgent() { return currentAgent; }
    public void setCurrentAgent(String currentAgent) { this.currentAgent = currentAgent; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public void addSubResult(String agentName, Object result) {
        this.subResults.put(agentName, result);
    }
}
