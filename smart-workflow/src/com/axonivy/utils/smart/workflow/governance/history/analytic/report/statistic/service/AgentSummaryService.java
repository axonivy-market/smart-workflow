package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder.ResponseMetadata;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

public class AgentSummaryService {

  private static final ObjectMapper MAPPER = JsonUtils.getObjectMapper();
  private static final String FIELD_MESSAGES = "messages";
  private static final String WARN_PARSE_MESSAGES  = "AgentSummaryService: failed to parse messagesJson: ";
  private static final String WARN_PARSE_METADATA  = "AgentSummaryService: failed to parse tokenUsageJson: ";

  private enum GuardrailResult {
    PASSED, SUCCESS, FAILED, FATAL, UNKNOWN;

    static GuardrailResult from(String result) {
      if (result == null) {
        return UNKNOWN;
      }
      for (var value : values()) {
        if (value.name().equalsIgnoreCase(result)) {
          return value;
        }
      }
      return UNKNOWN;
    }
  }

  private record MetadataSummary(int totalTokens, long durationMs, String finishReason, String model) {}

  private AgentSummaryService() {}

  public static AgentSummary summarizeAgent(AgentConversationEntry entry, List<ToolSummary> allToolSummaries,
      List<GuardrailSummary> allGuardrailSummaries, TokenUsageSummary tokenUsageSummary) {
    List<ToolExecution> tools     = Optional.ofNullable(entry.getToolExecutions()).orElse(List.of());
    List<GuardrailExecution> grds = Optional.ofNullable(entry.getGuardrailExecutions()).orElse(List.of());

    Map<String, ToolSummary> toolMap = allToolSummaries.stream()
        .collect(Collectors.toMap(ToolSummary::getToolName, summary -> summary, (existing, replacement) -> replacement));
    Map<String, GuardrailSummary> grdMap = allGuardrailSummaries.stream()
        .collect(Collectors.toMap(GuardrailSummary::getGuardrailName, summary -> summary, (existing, replacement) -> replacement));

    List<ToolSummary> toolSummaries = tools.stream()
        .map(ToolExecution::toolName).distinct()
        .map(toolMap::get).filter(summary -> summary != null).toList();
    List<GuardrailSummary> grdSummaries = grds.stream()
        .map(GuardrailExecution::guardrailName).distinct()
        .map(grdMap::get).filter(summary -> summary != null).toList();

    MetadataSummary meta = summarizeMetadata(parseMetadata(entry.getTokenUsageJson()));

    return new AgentSummary(entry.getAgentId(), entry.getAgentName(), entry.getProcessName(),
        countMessages(entry.getMessagesJson()),
        tools.size(), meta.totalTokens(), meta.durationMs(), meta.finishReason(), meta.model(),
        toolSummaries, grdSummaries, tokenUsageSummary,
        AnomalyDetectionService.detect(tools, toolSummaries, grdSummaries,
            meta.totalTokens(), meta.durationMs(), meta.finishReason()));
  }

  public static List<ToolSummary> summarizeTools(List<AgentConversationEntry> entries) {
    return entries.stream()
        .flatMap(entry -> Optional.ofNullable(entry.getToolExecutions()).orElse(List.of()).stream())
        .collect(Collectors.groupingBy(ToolExecution::toolName))
        .entrySet().stream()
        .map(group -> toToolSummary(group.getKey(), group.getValue()))
        .toList();
  }

  public static List<GuardrailSummary> summarizeGuardrails(List<AgentConversationEntry> entries) {
    return entries.stream()
        .flatMap(entry -> Optional.ofNullable(entry.getGuardrailExecutions()).orElse(List.of()).stream())
        .collect(Collectors.groupingBy(GuardrailExecution::guardrailName))
        .entrySet().stream()
        .map(group -> toGuardrailSummary(group.getKey(), group.getValue()))
        .toList();
  }

  public static TokenUsageSummary summarizeTokenUsage(List<AgentConversationEntry> entries) {
    List<ResponseMetadata> allMetadata = entries.stream()
        .flatMap(entry -> parseMetadata(entry.getTokenUsageJson()).stream())
        .toList();
    if (allMetadata.isEmpty()) {
      return new TokenUsageSummary(0, 0, 0, 0, 0, 0, 0);
    }
    int count = allMetadata.size();
    int totalInput = 0, totalOutput = 0, totalAll = 0, maxSingle = 0;
    for (var metadata : allMetadata) {
      totalInput  += safeInt(metadata.inputTokens());
      totalOutput += safeInt(metadata.outputTokens());
      int total    = safeInt(metadata.totalTokens());
      totalAll    += total;
      maxSingle    = Math.max(maxSingle, total);
    }
    return new TokenUsageSummary(
        totalInput, totalOutput, totalAll,
        totalInput / (double) count, totalOutput / (double) count, totalAll / (double) count,
        maxSingle);
  }

  private static MetadataSummary summarizeMetadata(List<ResponseMetadata> list) {
    if (list.isEmpty()) {
      return new MetadataSummary(0, 0L, null, null);
    }
    int totalTokens = 0;
    long durationMs = 0L;
    for (var metadata : list) {
      totalTokens += safeInt(metadata.totalTokens());
      durationMs  += safeLong(metadata.durationMs());
    }
    return new MetadataSummary(totalTokens, durationMs, list.getLast().finishReason(), list.getLast().modelName());
  }

  private static ToolSummary toToolSummary(String toolName, List<ToolExecution> executions) {
    int callCount       = executions.size();
    int nullResultCount = (int) executions.stream()
        .filter(execution -> StringUtils.isBlank(execution.resultText())).count();
    int errorCount      = (int) executions.stream()
        .filter(execution -> execution.resultText() != null && execution.resultText().toLowerCase().startsWith("error")).count();
    String sampleArguments = executions.stream()
        .map(ToolExecution::arguments).filter(arg -> !StringUtils.isBlank(arg))
        .findFirst().orElse(null);
    return new ToolSummary(toolName, callCount, nullResultCount, errorCount, sampleArguments);
  }

  private static GuardrailSummary toGuardrailSummary(String guardrailName, List<GuardrailExecution> executions) {
    int passedCount = 0, failedCount = 0, fatalCount = 0;
    long totalDurationMs = 0;
    int durationCount = 0;
    for (var execution : executions) {
      switch (GuardrailResult.from(execution.result())) {
        case PASSED, SUCCESS -> passedCount++;
        case FAILED          -> failedCount++;
        case FATAL           -> fatalCount++;
        default              -> {}
      }
      if (execution.durationMs() != null) {
        totalDurationMs += execution.durationMs();
        durationCount++;
      }
    }
    double avgDurationMs = durationCount > 0 ? (double) totalDurationMs / durationCount : 0;
    return new GuardrailSummary(guardrailName, passedCount, failedCount, fatalCount, avgDurationMs);
  }

  private static int countMessages(String messagesJson) {
    if (messagesJson == null || messagesJson.isBlank()) {
      return 0;
    }
    try {
      var node = MAPPER.readTree(messagesJson);
      if (node.isArray()) {
        return node.size();
      }
      var messages = node.path(FIELD_MESSAGES);
      return messages.isArray() ? messages.size() : 0;
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_PARSE_MESSAGES + e.getMessage());
      return 0;
    }
  }

  private static List<ResponseMetadata> parseMetadata(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(json, new TypeReference<List<ResponseMetadata>>() {});
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_PARSE_METADATA + e.getMessage());
      return List.of();
    }
  }

  private static int safeInt(Integer value) {
    return value != null ? value : 0;
  }

  private static long safeLong(Long value) {
    return value != null ? value : 0L;
  }
}
