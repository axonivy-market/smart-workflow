package com.axonivy.utils.smart.workflow.governance.history.entity;

import java.util.List;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class AgentConversationEntry {

  public record ToolExecution(
      @JsonProperty("toolName")   String toolName,
      @JsonProperty("arguments")  String arguments,
      @JsonProperty("resultText") String resultText,
      @JsonProperty("executedAt") String executedAt) {}

  private String caseUuid;
  private String taskUuid;
  private String agentId;
  private String messagesJson;
  private String tokenUsageJson;
  private String lastUpdated;
  private String toolExecutionsJson;

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getTaskUuid() { return taskUuid; }
  public void setTaskUuid(String taskUuid) { this.taskUuid = taskUuid; }

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }

  public String getMessagesJson() { return messagesJson; }
  public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

  public String getTokenUsageJson() { return tokenUsageJson; }
  public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }

  public String getLastUpdated() { return lastUpdated; }
  public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

  public String getToolExecutionsJson() { return toolExecutionsJson; }
  public void setToolExecutionsJson(String toolExecutionsJson) { this.toolExecutionsJson = toolExecutionsJson; }

  public List<ToolExecution> getToolExecutions() {
    return JsonUtils.jsonValueToEntities(toolExecutionsJson, ToolExecution.class);
  }

  public void setToolExecutions(List<ToolExecution> toolExecutions) {
    try {
      toolExecutionsJson = JsonUtils.getObjectMapper().writeValueAsString(toolExecutions);
    } catch (JsonProcessingException e) {
      toolExecutionsJson = null;
    }
  }
}
