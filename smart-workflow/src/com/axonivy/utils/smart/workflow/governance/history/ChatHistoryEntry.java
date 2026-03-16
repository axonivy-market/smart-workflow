package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;

import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ChatHistoryEntry {

  private String caseUuid;
  private String taskUuid;
  private String processName;
  private String messagesJson;
  private String tokenUsageJson;
  private LocalDateTime lastUpdated;

  @JsonIgnore
  private transient String caseDisplayName;
  @JsonIgnore
  private transient String taskDisplayName;

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getTaskUuid() { return taskUuid; }
  public void setTaskUuid(String taskUuid) { this.taskUuid = taskUuid; }

  public String getProcessName() { return processName; }
  public void setProcessName(String processName) { this.processName = processName; }

  public String getMessagesJson() { return messagesJson; }
  public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

  public String getTokenUsageJson() { return tokenUsageJson; }
  public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }

  public LocalDateTime getLastUpdated() { return lastUpdated; }
  public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

  @JsonIgnore
  public String getCaseDisplayName() {
    if (caseDisplayName == null) {
      caseDisplayName = CaseService.getDisplayName(caseUuid);
    }
    return caseDisplayName;
  }

  @JsonIgnore
  public String getTaskDisplayName() {
    if (taskDisplayName == null) {
      taskDisplayName = TaskService.getDisplayName(taskUuid);
    }
    return taskDisplayName;
  }

  @JsonIgnore
  public int getMessageCount() {
    return ChatHistoryJsonParser.getMessageCount(this);
  }

  @JsonIgnore
  public int getTotalTokens() {
    return ChatHistoryJsonParser.getTotalTokens(this);
  }

  @JsonIgnore
  public String getModelName() {
    return ChatHistoryJsonParser.getModelName(this);
  }
}
