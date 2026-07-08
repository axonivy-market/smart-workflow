package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.internal.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class TaskTreeNode {

  // Invariant: entries is always non-empty (both constructors guarantee this)
  private final List<AgentConversationEntry> entries;
  private final List<AgentTreeNode> agents;
  private boolean selected = false;

  public TaskTreeNode(AgentConversationEntry entry) {
    this(List.of(entry));
  }

  public TaskTreeNode(List<AgentConversationEntry> entries) {
    this.entries = entries.stream()
        .sorted(Comparator.comparing(AgentConversationEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
    this.agents = this.entries.stream().map(AgentTreeNode::new).toList();
  }

  public AgentConversationEntry getEntry() {
    return entries.get(0);
  }

  public List<AgentTreeNode> getAgents() {
    return agents;
  }

  public int getAgentCount() {
    return agents.size();
  }

  public String getTaskUuid() {
    return entries.get(0).getTaskUuid();
  }

  public String getDisplayName() {
    return TaskService.getDisplayName(entries.get(0).getTaskUuid());
  }

  public int getMessageCount() {
    return entries.stream().mapToInt(ChatHistoryJsonParser::getMessageCount).sum();
  }

  public long getTotalTokens() {
    return entries.stream().mapToLong(ChatHistoryJsonParser::getTotalTokens).sum();
  }

  public String getModelName() {
    return ChatHistoryJsonParser.getModelName(entries.get(0));
  }

  public long getAvgDurationMs() {
    return (long) entries.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .filter(duration -> duration > 0)
        .average()
        .orElse(0);
  }

  public LocalDateTime getLastUpdated() {
    return entries.stream()
        .map(entry -> DatePatternUtils.parseLastUpdated(entry.getLastUpdated()))
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  public String getLastUpdatedText() {
    LocalDateTime dt = getLastUpdated();
    return dt != null ? dt.format(DatePatternUtils.DISPLAY_FMT) : "—";
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}
