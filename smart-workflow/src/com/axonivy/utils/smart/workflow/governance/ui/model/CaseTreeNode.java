package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.CaseService;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class CaseTreeNode {

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
    for (Map.Entry<String, List<AgentConversationEntry>> caseGroup : byCase.entrySet()) {
      buildCaseNodes(caseGroup, nodes);
    }
    nodes.sort(Comparator.comparing(CaseTreeNode::getLastUpdated,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return nodes;
  }

  private static void buildCaseNodes(Map.Entry<String, List<AgentConversationEntry>> caseGroup, List<CaseTreeNode> nodes) {
    Map<String, List<AgentConversationEntry>> byTask = new LinkedHashMap<>();
      for (AgentConversationEntry entry : caseGroup.getValue()) {
        String taskKey = entry.getTaskUuid() != null ? entry.getTaskUuid() : "";
        byTask.computeIfAbsent(taskKey, k -> new ArrayList<>()).add(entry);
      }

      List<TaskTreeNode> taskNodes = buildTaskNodes(byTask);
      String processName = getProcessName(caseGroup);

      nodes.add(new CaseTreeNode(caseGroup.getKey(), processName, taskNodes));

  }

  private static List<TaskTreeNode> buildTaskNodes(Map<String, List<AgentConversationEntry>> byTask) {
    return byTask.values().stream()
          .map(TaskTreeNode::new)
          .sorted(Comparator.comparing(TaskTreeNode::getLastUpdated,
              Comparator.nullsLast(Comparator.naturalOrder())))
          .toList();
  }

  private static String getProcessName(Map.Entry<String, List<AgentConversationEntry>> caseGroup) {
    return caseGroup.getValue().stream()
        .map(AgentConversationEntry::getProcessName)
        .filter(name -> name != null && !name.isEmpty())
        .findFirst()
        .orElse("");
  }

  public String getCaseUuid() {
    return caseUuid;
  }

  public String getProcessName() {
    return processName;
  }

  public String getDisplayName() {
    return CaseService.getDisplayName(caseUuid);
  }

  public List<TaskTreeNode> getTasks() {
    return tasks;
  }

  public int getTaskCount() {
    return tasks.size();
  }

  public int getTotalMessages() {
    return tasks.stream().mapToInt(TaskTreeNode::getMessageCount).sum();
  }

  public long getTotalTokens() {
    return tasks.stream().mapToLong(TaskTreeNode::getTotalTokens).sum();
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
    return dt != null ? dt.format(DatePatternUtils.DISPLAY_FMT) : "—";
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}
