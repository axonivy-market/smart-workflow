package com.axonivy.utils.smart.workflow.governance.memory;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMemoryEntry {

  private String id;
  private String agentId;
  private String caseUuid;
  private String memoryId;
  private String messagesJson;
  private String tokenUsageJson;
  private LocalDateTime lastUpdated;

  public ChatMemoryEntry() {
    this.id = UUID.randomUUID().toString();
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getMemoryId() { return memoryId; }
  public void setMemoryId(String memoryId) { this.memoryId = memoryId; }

  public String getMessagesJson() { return messagesJson; }
  public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

  public String getTokenUsageJson() { return tokenUsageJson; }
  public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }

  public LocalDateTime getLastUpdated() { return lastUpdated; }
  public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}