package com.axonivy.utils.smart.workflow.governance.history.entity;

public class AgentConversationEntry {

  private String caseUuid;
  private String taskUuid;
  private String agentId;
  private String messagesJson;
  private String tokenUsageJson;
  private String lastUpdated;

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
}
