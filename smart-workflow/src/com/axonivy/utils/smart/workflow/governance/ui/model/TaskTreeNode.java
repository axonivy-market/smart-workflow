package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class TaskTreeNode {

  private final AgentConversationEntry entry;
  private boolean selected = false;

  public TaskTreeNode(AgentConversationEntry entry) {
    this.entry = entry;
  }

  public String getTaskUuid() { return entry.getTaskUuid(); }
  public String getDisplayName() { return TaskService.getDisplayName(entry.getTaskUuid()); }
  public int getMessageCount() { return ChatHistoryJsonParser.getMessageCount(entry); }
  public int getTotalTokens() { return ChatHistoryJsonParser.getTotalTokens(entry); }
  public String getModelName() { return ChatHistoryJsonParser.getModelName(entry); }

  public LocalDateTime getLastUpdated() {
    String s = entry.getLastUpdated();
    if (s == null) return null;
    try { return LocalDateTime.parse(s); } catch (Exception e) { return null; }
  }

  public String getLastUpdatedText() { return entry.getLastUpdatedText(); }
  public long getAvgDurationMs() { return ChatHistoryJsonParser.getAvgDurationMs(entry); }
  public AgentConversationEntry getEntry() { return entry; }
  public boolean isSelected() { return selected; }
  public void setSelected(boolean selected) { this.selected = selected; }
}
