package com.axonivy.utils.smart.workflow.governance.history.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser.TokenUsage;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class AgentConversationEntry {

  public record ToolExecution(
      @JsonProperty("toolName")   String toolName,
      @JsonProperty("arguments")  String arguments,
      @JsonProperty("resultText") String resultText,
      @JsonProperty("executedAt") String executedAt) {}

  public record GuardrailExecution(
      @JsonProperty("guardrailName")  String guardrailName,
      @JsonProperty("type")           String type,
      @JsonProperty("result")         String result,
      @JsonProperty("message")        String message,
      @JsonProperty("failureMessage") String failureMessage,
      @JsonProperty("durationMs")     Long durationMs,
      @JsonProperty("executedAt")     String executedAt) {}

  private String caseUuid;
  private String taskUuid;
  private String agentId;
  private String agentName;
  private String processName;
  private String messagesJson;
  private String tokenUsageJson;
  private String lastUpdated;
  private String toolExecutionsJson;
  private String guardrailExecutionsJson;

  @JsonIgnore
  private TokenUsage tokenUsage;

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getTaskUuid() { return taskUuid; }
  public void setTaskUuid(String taskUuid) { this.taskUuid = taskUuid; }

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }

  public String getAgentName() { return agentName; }
  public void setAgentName(String agentName) { this.agentName = agentName; }

  public String getProcessName() { return processName; }
  public void setProcessName(String processName) { this.processName = processName; }

  public String getMessagesJson() { return messagesJson; }
  public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

  public String getTokenUsageJson() { return tokenUsageJson; }
  public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }

  public String getLastUpdated() { return lastUpdated; }
  public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

  @JsonIgnore
  public String getLastUpdatedRaw() { return lastUpdated != null ? lastUpdated : ""; }

  @JsonIgnore
  public String getLastUpdatedText() {
    if (lastUpdated == null) return "—";
    try {
      return LocalDateTime.parse(lastUpdated)
          .format(DatePatternUtils.dateTimeFormatter());
    } catch (Exception e) {
      return lastUpdated;
    }
  }

  @JsonIgnore
  public int getMessageCount() {
    return ChatHistoryJsonParser.getMessageCount(this);
  }

  @JsonIgnore
  public int getTotalTokens() {
    return tokenUsage().totalTokens();
  }

  @JsonIgnore
  public String getModelName() {
    return tokenUsage().modelName();
  }

  private TokenUsage tokenUsage() {
    if (tokenUsage == null) {
      tokenUsage = ChatHistoryJsonParser.parseTokenUsage(this);
    }
    return tokenUsage;
  }

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

  public String getGuardrailExecutionsJson() { return guardrailExecutionsJson; }
  public void setGuardrailExecutionsJson(String guardrailExecutionsJson) { this.guardrailExecutionsJson = guardrailExecutionsJson; }

  public List<GuardrailExecution> getGuardrailExecutions() {
    return JsonUtils.jsonValueToEntities(guardrailExecutionsJson, GuardrailExecution.class);
  }

  public void setGuardrailExecutions(List<GuardrailExecution> guardrailExecutions) {
    try {
      guardrailExecutionsJson = JsonUtils.getObjectMapper().writeValueAsString(guardrailExecutions);
    } catch (JsonProcessingException e) {
      guardrailExecutionsJson = null;
    }
  }
}
