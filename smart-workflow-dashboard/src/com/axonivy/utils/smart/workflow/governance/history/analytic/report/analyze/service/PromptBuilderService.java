package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.langchain4j.model.input.PromptTemplate;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service.CaseStatisticsService;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;

class PromptBuilderService {

  private static final String WARN_METRICS_FAILURE = "PromptBuilderService: failed to serialize CaseStatistics: ";

  private static final String TEMPLATE = """
      You are an AI governance analyst. Based on the structured metrics and raw conversations below,
      identify anomalies, assess agent performance, and flag any concerns.

      <metrics>
      {{metrics}}
      </metrics>

      <conversations>
      {{conversations}}
      </conversations>

      Respond with a structured analysis covering: anomalies detected, agent grades, and recommended actions.""";

  private static final String AGENT_HEADER       = "--- Agent %d: %s | %dms | %d tokens ---\n";
  private static final String MSG_DATA_UNAVAILABLE = "  (conversation data unavailable)\n";
  private static final String TOOL_FORMAT        = "  [TOOL] %s: %s\n";
  private static final String GUARDRAIL_FORMAT   = "  [GUARDRAIL] %s: %s\n";

  private PromptBuilderService() {}

  public static String build(String caseId, String caseName, List<AgentSummary> summaries,
      List<AgentConversationEntry> entries) {
    return PromptTemplate.from(TEMPLATE)
        .apply(Map.of(
            "metrics", buildMetrics(caseId, caseName, summaries),
            "conversations", buildConversations(summaries, entries)))
        .text();
  }

  private static String buildMetrics(String caseId, String caseName, List<AgentSummary> summaries) {
    try {
      return JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValueAsString(CaseStatisticsService.compute(caseId, caseName, summaries));
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_METRICS_FAILURE + e.getMessage());
      return "{}";
    }
  }

  private static String buildConversations(List<AgentSummary> summaries, List<AgentConversationEntry> entries) {
    if (entries == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < entries.size(); i++) {
      AgentSummary summary = summaries != null && i < summaries.size() ? summaries.get(i) : null;
      sb.append(buildEntryConversation(i, entries.get(i), summary));
    }
    return sb.toString();
  }

  private static String buildEntryConversation(int index, AgentConversationEntry entry, AgentSummary summary) {
    long durationMs = summary != null ? summary.getDurationMs() : 0;
    int totalTokens = summary != null ? summary.getTotalTokens() : 0;
    StringBuilder sb = new StringBuilder();
    sb.append(AGENT_HEADER.formatted(index + 1, entry.getDisplayName(), durationMs, totalTokens));
    sb.append(formatConversation(entry));
    sb.append(formatToolExecutions(entry));
    sb.append(formatGuardrailExecutions(entry));
    sb.append("\n");
    return sb.toString();
  }

  private static String formatConversation(AgentConversationEntry entry) {
    var parsed = ConversationParserService.parse(entry);
    if (parsed.isEmpty()) {
      return MSG_DATA_UNAVAILABLE;
    }
    return parsed.get();
  }

  private static String formatToolExecutions(AgentConversationEntry entry) {
    List<AgentConversationEntry.ToolExecution> tools = entry.getToolExecutions();
    if (tools == null || tools.isEmpty()) {
      return "";
    }
    return tools.stream()
        .map(tool -> TOOL_FORMAT.formatted(tool.toolName(), tool.resultText() != null ? tool.resultText() : ""))
        .collect(Collectors.joining());
  }

  private static String formatGuardrailExecutions(AgentConversationEntry entry) {
    List<AgentConversationEntry.GuardrailExecution> guardrails = entry.getGuardrailExecutions();
    if (guardrails == null || guardrails.isEmpty()) {
      return "";
    }
    return guardrails.stream()
        .map(guardrail -> GUARDRAIL_FORMAT.formatted(guardrail.guardrailName(), guardrail.result()))
        .collect(Collectors.joining());
  }
}
