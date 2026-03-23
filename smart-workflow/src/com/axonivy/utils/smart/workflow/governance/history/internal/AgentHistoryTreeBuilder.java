package com.axonivy.utils.smart.workflow.governance.history.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder;
import com.axonivy.utils.smart.workflow.utils.DateParsingUtils;


public class AgentHistoryTreeBuilder {

  public record AgentNode(AgentConversationEntry chat, List<ToolExecutionEntry> tools, List<AgentNode> children) {}

  public record TaskNode(String taskUuid, List<AgentNode> agents) {}

  public record CaseNode(String caseUuid, List<TaskNode> tasks) {}

  /**
   * Builds a Case > Task > Agent > Tool tree from the given history entries.
   */
  public static List<CaseNode> buildTree(List<AgentConversationEntry> chatEntries, List<ToolExecutionEntry> toolEntries) {

    var chatEntriesByCase = groupBy(chatEntries, AgentConversationEntry::getCaseUuid);
    var toolEntriesByCase = groupBy(toolEntries, ToolExecutionEntry::getCaseUuid);
    var allCaseUuids = Stream.concat(chatEntriesByCase.keySet().stream(), toolEntriesByCase.keySet().stream())
        .distinct();

    return allCaseUuids.map(caseUuid -> buildCaseNode(caseUuid,
            chatEntriesByCase.getOrDefault(caseUuid, List.of()),
            toolEntriesByCase.getOrDefault(caseUuid, List.of())))
        .toList();
  }

  private static CaseNode buildCaseNode(String caseUuid,
    List<AgentConversationEntry> chatEntries,
    List<ToolExecutionEntry> toolEntries) {

    var chatEntriesByTask = groupBy(chatEntries, entry -> resolveTaskUuid(entry.getTaskUuid()));
    var toolEntriesByTask = groupBy(toolEntries, entry -> resolveTaskUuid(entry.getTaskUuid()));

    var allTaskUuids = Stream.concat(chatEntriesByTask.keySet().stream(), toolEntriesByTask.keySet().stream())
        .distinct();
    List<TaskNode> taskNodes = sortTaskNodesByAgentTimestampAsc(allTaskUuids.map(taskUuid -> new TaskNode(
            taskUuid,
            buildAgentNodes(chatEntriesByTask.getOrDefault(taskUuid, List.of()),
            toolEntriesByTask.getOrDefault(taskUuid, List.of()))))
        .toList());

    return new CaseNode(caseUuid, taskNodes);
  }

  private static List<AgentNode> buildAgentNodes(List<AgentConversationEntry> chatEntries, List<ToolExecutionEntry> toolEntries) {

    Map<String, List<AgentConversationEntry>> subAgentMap = SubAgentMapper.map(chatEntries, toolEntries);

    Set<String> subAgentIds = subAgentMap.values().stream()
        .flatMap(List::stream)
        .map(AgentConversationEntry::getAgentId)
        .collect(Collectors.toSet());

    return chatEntries.stream()
        .filter(entry -> !subAgentIds.contains(entry.getAgentId()))
        .sorted(Comparator.comparing(AgentConversationEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(entry -> buildAgentNode(entry, toolEntries, subAgentMap))
        .toList();
  }

  private static AgentNode buildAgentNode(AgentConversationEntry chat, List<ToolExecutionEntry> allTools,
      Map<String, List<AgentConversationEntry>> subAgentMap) {
    return new AgentNode(chat, toolsForAgent(chat.getAgentId(), allTools), buildSubAgentNodes(chat.getAgentId(), subAgentMap, allTools));
  }

  private static List<ToolExecutionEntry> toolsForAgent(String agentId, List<ToolExecutionEntry> allTools) {
    return allTools.stream()
        .filter(tool -> agentId.equals(tool.getAgentId()))
        .sorted(Comparator.comparing(ToolExecutionEntry::getExecutedAt,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private static List<AgentNode> buildSubAgentNodes(String agentId,
      Map<String, List<AgentConversationEntry>> subAgentMap,
      List<ToolExecutionEntry> allTools) {
    return CollectionUtils.emptyIfNull(subAgentMap.get(agentId)).stream()
        .sorted(Comparator.comparing(AgentConversationEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(child -> buildAgentNode(child, allTools, subAgentMap))
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
                .map(DateParsingUtils::parse)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null),
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private static <T> Map<String, List<T>> groupBy(List<T> entries, Function<T, String> key) {
    return entries.stream().collect(Collectors.groupingBy(key));
  }

  private static String resolveTaskUuid(String taskUuid) {
    return StringUtils.defaultIfBlank(taskUuid, HistoryRecorder.NO_TASK_UUID);
  }
}
