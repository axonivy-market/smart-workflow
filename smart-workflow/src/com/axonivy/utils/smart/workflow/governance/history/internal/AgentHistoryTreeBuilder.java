package com.axonivy.utils.smart.workflow.governance.history.internal;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;


public class AgentHistoryTreeBuilder {

  public record AgentNode(AgentConversationEntry chat, List<ToolExecution> tools) {}

  public record TaskNode(String taskUuid, List<AgentNode> agents) {}

  public record CaseNode(String caseUuid, List<TaskNode> tasks) {}

  /**
   * Builds a Case > Task > Agent > Tool tree from the given history entries.
   */
  public static List<CaseNode> buildTree(List<AgentConversationEntry> entries) {
    var entriesByCase = groupBy(entries, AgentConversationEntry::getCaseUuid);
    return entriesByCase.entrySet().stream()
        .map(caseGroup -> buildCaseNode(caseGroup.getKey(), caseGroup.getValue()))
        .toList();
  }

  private static CaseNode buildCaseNode(String caseUuid, List<AgentConversationEntry> entries) {
    var entriesByTask = groupBy(entries, AgentConversationEntry::getTaskUuid);
    List<TaskNode> taskNodes = sortTaskNodesByAgentTimestampAsc(entriesByTask.entrySet().stream()
        .map(taskGroup -> new TaskNode(taskGroup.getKey(), buildAgentNodes(taskGroup.getValue())))
        .toList());
    return new CaseNode(caseUuid, taskNodes);
  }

  private static List<AgentNode> buildAgentNodes(List<AgentConversationEntry> entries) {
    return entries.stream()
        .sorted(Comparator.comparing(AgentConversationEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(entry -> new AgentNode(entry, entry.getToolExecutions()))
        .toList();
  }

  private static List<TaskNode> sortTaskNodesByAgentTimestampAsc(List<TaskNode> tasks) {
    return tasks.stream()
        .sorted(Comparator.comparing(
            task -> task.agents().stream()
                .map(AgentNode::chat)
                .filter(Objects::nonNull)
                .map(AgentConversationEntry::getLastUpdated)
                .filter(Objects::nonNull)
                .map(timestamp -> StringUtils.isBlank(timestamp) ? null : LocalDateTime.parse(timestamp))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null),
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private static <T> Map<String, List<T>> groupBy(List<T> entries, Function<T, String> key) {
    return entries.stream().collect(Collectors.groupingBy(key));
  }
}
