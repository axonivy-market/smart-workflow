package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service.CaseStatisticsService;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

class PromptBuilderService {

  private static final String WARN_METRICS_FAILURE = "PromptBuilderService: failed to serialize CaseStatistics: ";
  private static final String WARN_PARSE_FAILURE = "PromptBuilderService: failed to parse messagesJson: ";

  private interface Field {
    String MESSAGES = "messages";
    String TEXT     = "text";
    String TYPE     = "type";
    String CONTENTS = "contents";
    String METRICS  = "metrics";
    String CONVERSATIONS = "conversations";
  }

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
  private static final String TYPE_TEXT          = "TEXT";
  private static final String TYPE_UNKNOWN       = "UNKNOWN";
  private static final String TOOL_FORMAT        = "  [TOOL] %s: %s\n";
  private static final String GUARDRAIL_FORMAT   = "  [GUARDRAIL] %s: %s\n";

  private PromptBuilderService() {}

  public static String build(String caseId, String caseName, List<AgentSummary> summaries,
      List<AgentConversationEntry> entries) {
    return PromptTemplate.from(TEMPLATE)
        .apply(Map.of(
            Field.METRICS, buildMetrics(caseId, caseName, summaries),
            Field.CONVERSATIONS, buildConversations(summaries, entries)))
        .text();
  }

  private static String buildMetrics(String caseId, String caseName, List<AgentSummary> summaries) {
    try {
      return JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValueAsString(CaseStatisticsService.compute(caseId, caseName, summaries));
    } catch (JacksonException e) {
      Ivy.log().warn(WARN_METRICS_FAILURE + e.getMessage());
      return "{}";
    }
  }

  private static String buildConversations(List<AgentSummary> summaries, List<AgentConversationEntry> entries) {
    if (entries == null) {
      return "";
    }
    IntPredicate hasSummaryAt = index->
      Optional.of(summaries).map(s -> s.get(index)).isPresent() 
      && index < summaries.size();

    return IntStream.range(0, entries.size())
        .mapToObj(i -> {
          AgentSummary summary = hasSummaryAt.test(i) ? summaries.get(i) : null;
          return buildEntryConversation(i, entries.get(i), summary);
        })
        .collect(Collectors.joining());
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
    try {
      var node = JsonUtils.getObjectMapper().readTree(entry.getMessagesJson());
      return buildConversationText(node.isArray() ? node : node.path(Field.MESSAGES));
    } catch (JacksonException e) {
      Ivy.log().warn(WARN_PARSE_FAILURE + e.getMessage());
      return MSG_DATA_UNAVAILABLE;
    }
  }

  private static String buildConversationText(JsonNode messages) {
    if (!messages.isArray()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    for (var message : messages) {
      String type = message.path(Field.TYPE).asString(TYPE_UNKNOWN).toUpperCase(Locale.ROOT);
      String messageText = extractText(message);
      if (messageText != null && !messageText.isBlank()) {
        text.append("[").append(type).append("] ").append(messageText).append("\n");
      }
    }
    return text.toString();
  }

  private static String extractText(JsonNode message) {
    var textNode = message.path(Field.TEXT);
    if (!textNode.isMissingNode()) {
      return textNode.asString();
    }
    var contents = message.path(Field.CONTENTS);
    if (contents.isArray()) {
      StringBuilder text = new StringBuilder();
      for (var content : contents) {
        var contentText = content.path(Field.TEXT);
        if (!contentText.isMissingNode() && TYPE_TEXT.equalsIgnoreCase(content.path(Field.TYPE).asString(""))) {
          text.append(contentText.asString());
        }
      }
      return !text.isEmpty() ? text.toString() : null;
    }
    return null;
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
