package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.CaseService;

public class CaseTreeNode {

  private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

  private final String caseUuid;
  private final String processName;
  private final List<TaskTreeNode> tasks;
  private boolean expanded = true;
  private boolean selected = false;

  public CaseTreeNode(String caseUuid, String processName, List<TaskTreeNode> tasks) {
    this.caseUuid = caseUuid;
    this.processName = processName;
    this.tasks = tasks;
  }

  /**
   * Factory: groups entries by caseUuid, sorts cases and tasks by lastUpdated descending
   * (newest first).
   */
  public static List<CaseTreeNode> buildTree(List<AgentConversationEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      return List.of();
    }
    Map<String, List<AgentConversationEntry>> byCase = new LinkedHashMap<>();
    for (AgentConversationEntry entry : entries) {
      String key = entry.getCaseUuid() != null ? entry.getCaseUuid() : "";
      byCase.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    }
    List<CaseTreeNode> nodes = new ArrayList<>();
    for (Map.Entry<String, List<AgentConversationEntry>> e : byCase.entrySet()) {
      List<TaskTreeNode> taskNodes = e.getValue().stream()
          .sorted(Comparator.comparing(AgentConversationEntry::getLastUpdated,
              Comparator.nullsLast(Comparator.naturalOrder())))
          .map(TaskTreeNode::new)
          .toList();
      String processName = e.getValue().stream()
          .map(AgentConversationEntry::getProcessName)
          .filter(n -> n != null && !n.isEmpty())
          .findFirst()
          .orElse("");
      nodes.add(new CaseTreeNode(e.getKey(), processName, taskNodes));
    }
    nodes.sort(Comparator.comparing(CaseTreeNode::getLastUpdated,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return nodes;
  }

  public String getCaseUuid() { return caseUuid; }
  public String getProcessName() { return processName; }
  public String getDisplayName() { return CaseService.getDisplayName(caseUuid); }
  public List<TaskTreeNode> getTasks() { return tasks; }
  public int getTaskCount() { return tasks.size(); }

  public int getTotalMessages() {
    return tasks.stream().mapToInt(TaskTreeNode::getMessageCount).sum();
  }

  public int getTotalTokens() {
    return tasks.stream().mapToInt(TaskTreeNode::getTotalTokens).sum();
  }

  public LocalDateTime getLastUpdated() {
    return tasks.stream()
        .map(TaskTreeNode::getLastUpdated)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  public String getTopModelName() {
    return tasks.isEmpty() ? "—" : tasks.get(0).getModelName();
  }

  public String getLastUpdatedText() {
    LocalDateTime dt = getLastUpdated();
    return dt != null ? dt.format(DISPLAY_FMT) : "—";
  }

  public boolean isExpanded() { return expanded; }
  public void setExpanded(boolean expanded) { this.expanded = expanded; }
  public boolean isSelected() { return selected; }
  public void setSelected(boolean selected) { this.selected = selected; }
}
