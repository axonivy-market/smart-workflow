package com.axonivy.utils.smart.workflow.governance.memory;

import java.time.LocalDateTime;

public class ChatMemoryEntry {

  private String caseUuid;
  private String taskUuid;
  private String messagesJson;
  private String tokenUsageJson;
  private LocalDateTime lastUpdated;

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getTaskUuid() { return taskUuid; }
  public void setTaskUuid(String taskUuid) { this.taskUuid = taskUuid; }

  public String getMessagesJson() { return messagesJson; }
  public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

  public String getTokenUsageJson() { return tokenUsageJson; }
  public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }

  public LocalDateTime getLastUpdated() { return lastUpdated; }
  public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
