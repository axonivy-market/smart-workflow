package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class TaskTreeNode {

  private final List<AgentConversationEntry> entries;
  private final List<AgentTreeNode> agents;
  private boolean selected = false;

  /** Single-entry constructor — backward compat for cases with one agent per task. */
  public TaskTreeNode(AgentConversationEntry entry) {
    this(List.of(entry));
  }

  /** Multi-entry constructor — all entries must share the same taskUuid. */
  public TaskTreeNode(List<AgentConversationEntry> entries) {
    this.entries = entries.stream()
        .sorted(Comparator.comparing(ChatHistoryJsonParser::getStartTimestamp,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
    this.agents = this.entries.stream().map(AgentTreeNode::new).toList();
  }

  /** First entry — used by export and legacy callers. */
  public AgentConversationEntry getEntry() { return entries.get(0); }

  public List<AgentTreeNode> getAgents() { return agents; }
  public int getAgentCount() { return agents.size(); }

  public String getTaskUuid() { return entries.get(0).getTaskUuid(); }
  public String getDisplayName() { return TaskService.getDisplayName(entries.get(0).getTaskUuid()); }

  public int getMessageCount() {
    return entries.stream().mapToInt(ChatHistoryJsonParser::getMessageCount).sum();
  }

  public int getTotalTokens() {
    return entries.stream().mapToInt(ChatHistoryJsonParser::getTotalTokens).sum();
  }

  public String getModelName() { return ChatHistoryJsonParser.getModelName(entries.get(0)); }

  public long getAvgDurationMs() {
    return (long) entries.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .filter(d -> d > 0)
        .average()
        .orElse(0);
  }

  public LocalDateTime getLastUpdated() {
    return entries.stream()
        .map(e -> {
          String s = e.getLastUpdated();
          if (s == null) return null;
          try { return LocalDateTime.parse(s); } catch (Exception ex) { return null; }
        })
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  public String getLastUpdatedText() { return entries.get(0).getLastUpdatedText(); }

  public boolean isSelected() { return selected; }
  public void setSelected(boolean selected) { this.selected = selected; }
}
