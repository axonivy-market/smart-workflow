package com.axonivy.utils.smart.workflow.governance.history;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Correlates sub-agent {@link ChatHistoryEntry} records with the parent
 * {@link ToolExecutionEntry} that triggered them, using timestamp proximity.
 *
 * <p>Precondition: no concurrent agent executions for the same caseUuid+taskUuid.
 *
 * <p>Matching rule: for each tool, the sub-agent with the minimum gap
 * {@code executedAt - lastUpdated} (where {@code executedAt >= lastUpdated}) is selected.
 * This ensures one-to-one matching and prevents standalone agents (e.g. an OCR agent that ran
 * before the orchestrator) from stealing tool slots from the real sub-agents.
 */
public class AgentHistoryCorrelator {

  public record AgentLink(ChatHistoryEntry subAgent, ToolExecutionEntry triggerTool) {}

  /**
   * Returns one {@link AgentLink} per tool entry that has a matching sub-agent.
   * Tools with no matching sub-agent are omitted. Each sub-agent can match at most one tool.
   */
  public static List<AgentLink> correlate(
      List<ChatHistoryEntry> chatEntries,
      List<ToolExecutionEntry> toolEntries) {

    Set<String> parentAgentIds = toolEntries.stream()
        .map(ToolExecutionEntry::getAgentId)
        .collect(Collectors.toSet());

    List<ChatHistoryEntry> subAgentCandidates = chatEntries.stream()
        .filter(e -> !parentAgentIds.contains(e.getAgentId()))
        .toList();

    return toolEntries.stream()
        .map(tool -> findBestSubAgent(tool, subAgentCandidates)
            .map(sub -> new AgentLink(sub, tool)).orElse(null))
        .filter(link -> link != null)
        .toList();
  }

  private static Optional<ChatHistoryEntry> findBestSubAgent(
      ToolExecutionEntry tool, List<ChatHistoryEntry> candidates) {
    return candidates.stream()
        .filter(c -> c.getLastUpdated() != null && tool.getExecutedAt() != null
            && !tool.getExecutedAt().isBefore(c.getLastUpdated()))
        .min(Comparator.comparing(
            c -> Duration.between(c.getLastUpdated(), tool.getExecutedAt())));
  }

  // --- Sequence ---

  public record AgentNode(
      ChatHistoryEntry chat,
      List<ToolExecutionEntry> tools,
      List<AgentNode> children) {}

  /**
   * Builds an ordered sequence of {@link AgentNode} trees from flat chat and tool entry lists.
   * Each chat entry that is not a sub-agent of any tool becomes a root.
   * Roots are sorted by {@code chat.lastUpdated} ascending, reflecting the order in which
   * agents were called by the Ivy process.
   * Standalone agents (not in any tool chain) appear as leaf roots in the sequence.
   */
  public static List<AgentNode> buildSequence(
      List<ChatHistoryEntry> chatEntries,
      List<ToolExecutionEntry> toolEntries) {

    List<AgentLink> links = correlate(chatEntries, toolEntries);

    Set<String> subAgentIds = links.stream()
        .map(l -> l.subAgent().getAgentId())
        .collect(Collectors.toSet());

    Map<String, List<AgentLink>> linksByParent = links.stream()
        .collect(Collectors.groupingBy(l -> l.triggerTool().getAgentId()));

    return chatEntries.stream()
        .filter(e -> !subAgentIds.contains(e.getAgentId()))
        .map(root -> buildNode(root, toolEntries, linksByParent))
        .sorted(Comparator.comparing(n -> n.chat().getLastUpdated(),
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private static AgentNode buildNode(
      ChatHistoryEntry chat,
      List<ToolExecutionEntry> allTools,
      Map<String, List<AgentLink>> linksByParent) {

    List<ToolExecutionEntry> myTools = allTools.stream()
        .filter(t -> chat.getAgentId().equals(t.getAgentId()))
        .sorted(Comparator.comparing(ToolExecutionEntry::getExecutedAt,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();

    List<AgentNode> children = linksByParent
        .getOrDefault(chat.getAgentId(), List.of()).stream()
        .map(link -> buildNode(link.subAgent(), allTools, linksByParent))
        .toList();

    return new AgentNode(chat, myTools, children);
  }
}
