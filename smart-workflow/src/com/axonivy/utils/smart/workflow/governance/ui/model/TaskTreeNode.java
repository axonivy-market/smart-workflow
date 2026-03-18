package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryEntry;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class TaskTreeNode {

  private final ChatHistoryEntry entry;
  private boolean selected = false;

  public TaskTreeNode(ChatHistoryEntry entry) {
    this.entry = entry;
  }

  public String getTaskUuid() { return entry.getTaskUuid(); }
  public String getDisplayName() { return TaskService.getDisplayName(entry.getTaskUuid()); }
  public int getMessageCount() { return ChatHistoryJsonParser.getMessageCount(entry); }
  public int getTotalTokens() { return ChatHistoryJsonParser.getTotalTokens(entry); }
  public String getModelName() { return ChatHistoryJsonParser.getModelName(entry); }
  public LocalDateTime getLastUpdated() { return entry.getLastUpdated(); }
  public String getLastUpdatedText() { return entry.getLastUpdatedText(); }
  public long getAvgDurationMs() { return ChatHistoryJsonParser.getAvgDurationMs(entry); }
  public ChatHistoryEntry getEntry() { return entry; }
  public boolean isSelected() { return selected; }
  public void setSelected(boolean selected) { this.selected = selected; }
}
