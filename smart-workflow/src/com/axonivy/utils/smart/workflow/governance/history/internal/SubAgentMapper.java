package com.axonivy.utils.smart.workflow.governance.history.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.utils.DateParsingUtils;

import ch.ivyteam.ivy.environment.Ivy;

class SubAgentMapper {

  static Map<String, List<AgentConversationEntry>> map(
      List<AgentConversationEntry> chatEntries,
      List<ToolExecutionEntry> toolEntries) {

    Set<String> callerAgentIds = toolEntries.stream()
        .map(ToolExecutionEntry::getAgentId)
        .collect(Collectors.toSet());

    List<AgentConversationEntry> subAgentCandidates = chatEntries.stream()
        .filter(entry -> !callerAgentIds.contains(entry.getAgentId()))
        .toList();

    return matchToolsToSubAgents(toolEntries, subAgentCandidates);
  }

  private static Map<String, List<AgentConversationEntry>> matchToolsToSubAgents(
      List<ToolExecutionEntry> tools,
      List<AgentConversationEntry> candidates) {

    List<ToolExecutionEntry> sortedTools = tools.stream()
        .filter(tool -> tool.getExecutedAt() != null)
        .sorted(Comparator.comparing(tool -> DateParsingUtils.parse(tool.getExecutedAt())))
        .toList();

    List<AgentConversationEntry> conversationEntries = new ArrayList<>(candidates.stream()
        .filter(candidate -> candidate.getLastUpdated() != null)
        .sorted(Comparator.comparing(
            candidate -> DateParsingUtils.parse(candidate.getLastUpdated()),
            Comparator.reverseOrder()))
        .toList());

    Map<String, List<AgentConversationEntry>> childAgentsByCaller = new HashMap<>();

    for (ToolExecutionEntry tool : sortedTools) {
      var match = conversationEntries.stream()
          .filter(candidate -> !DateParsingUtils.parse(tool.getExecutedAt())
              .isBefore(DateParsingUtils.parse(candidate.getLastUpdated())))
          .findFirst();

      if (match.isPresent()) {
        childAgentsByCaller.computeIfAbsent(tool.getAgentId(), agentId -> new ArrayList<>()).add(match.get());
        conversationEntries.remove(match.get());
      } else {
        Ivy.log().warn(String.format("No sub-agent matched tool '%s' executedAt=%s",
            tool.getToolName(), tool.getExecutedAt()));
      }
    }

    return childAgentsByCaller;
  }
}
