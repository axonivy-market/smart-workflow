package com.axonivy.utils.smart.workflow.governance.history.analytic.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.AnomalyReport;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder.ResponseMetadata;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;

public class CaseHistoryAnalyzer {

  private static final IvyRepoHistoryStorage STORAGE = new IvyRepoHistoryStorage();

  private static final String NO_DATA_MSG                = "No agent data available for case %s.";

  private static final String SECTION_METRICS            = "=== STRUCTURED METRICS REPORT ===\n\n";
  private static final String SECTION_EXCERPTS_HEADER    = """
      === RAW CONVERSATION EXCERPTS ===

      (System prompt + final AI response per agent, truncated for brevity)

      """;
  private static final String SECTION_END                = "=== END OF DATA ===\n";

  private static final String AGENT_HEADER               = "--- Agent %d: %s ---\n";
  private static final String MSG_SYSTEM                 = "  [SYSTEM] %s\n";
  private static final String MSG_AI_RESPONSE            = "  [FINAL AI RESPONSE] %s\n";
  private static final String MSG_DATA_UNAVAILABLE       = "  (conversation data unavailable)\n";
  private static final String MSG_TOOLS_PREFIX           = "  [TOOLS] ";
  private static final String MSG_GUARDRAILS_PREFIX      = "  [GUARDRAILS] ";

  private static final String ANOMALY_DURATION           = "Duration exceeded 30s (%dms)";
  private static final String ANOMALY_TOKENS             = "Tokens exceeded 10,000 per conversation (%d)";
  private static final String ANOMALY_TOOL_CALLS         = "More than 10 tool calls (%d) \u2014 possible loop";
  private static final String ANOMALY_FATAL_GUARDRAIL    = "FATAL guardrail result on '%s' (%d time(s))";
  private static final String ANOMALY_LENGTH             = "Finish reason is LENGTH \u2014 output was truncated";
  private static final String ANOMALY_NULL_RESULT        = "Tool '%s' returned null/empty in %d/%d calls (>50%%)";
  private static final String ANOMALY_STUCK_LOOP         = "Tool '%s' called %d times with identical arguments \u2014 possible stuck loop";

  public static List<AgentSummary> analyze(String caseId) {
    List<AgentConversationEntry> entries = STORAGE.findByCaseUuid(caseId);
    List<ToolSummary> allToolSummaries = buildToolSummaries(entries);
    List<GuardrailSummary> allGuardrailSummaries = buildGuardrailSummaries(entries);
    TokenUsageSummary tokenUsageSummary = buildTokenUsageSummary(entries);
    List<AgentSummary> summaries = entries.stream()
        .map(e -> summarize(e, allToolSummaries, allGuardrailSummaries, tokenUsageSummary))
        .toList();
    Ivy.log().info("Summaries for caseId {0}: {1}", caseId, summaries);
    return summaries;
  }

  public static int countAgents(List<AgentSummary> summaries) {
    return summaries != null ? summaries.size() : 0;
  }

  public static int totalCaseTokens(List<AgentSummary> summaries) {
    if (summaries == null) return 0;
    return summaries.stream().mapToInt(AgentSummary::getTotalTokens).sum();
  }

  public static long totalCaseDurationMs(List<AgentSummary> summaries) {
    if (summaries == null) return 0L;
    return summaries.stream().mapToLong(AgentSummary::getDurationMs).sum();
  }

  public static int countAnomalyAgents(List<AgentSummary> summaries) {
    if (summaries == null) return 0;
    return (int) summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues())
        .count();
  }

  public static String toJson(List<AgentSummary> summaries) {
    try {
      return JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(summaries);
    } catch (Exception e) {
      return "[]";
    }
  }

  public static List<AgentConversationEntry> getEntries(String caseId) {
    return STORAGE.findByCaseUuid(caseId);
  }

  public static String buildAiPrompt(String caseId, List<AgentSummary> summaries,
      List<AgentConversationEntry> entries) {
    if (summaries == null || summaries.isEmpty()) {
      return NO_DATA_MSG.formatted(caseId);
    }

    String caseName = resolveCaseName(caseId);
    StringBuilder sb = new StringBuilder();

    sb.append(SECTION_METRICS);
    sb.append(new AnalyticReportBuilder(caseId, caseName, summaries).render());
    sb.append("\n\n");
    sb.append(SECTION_EXCERPTS_HEADER);

    if (entries != null) {
      for (int i = 0; i < entries.size(); i++) {
        AgentConversationEntry entry = entries.get(i);
        String agentLabel = (entry.getAgentName() != null && !entry.getAgentName().isBlank())
            ? entry.getAgentName() : entry.getAgentId();
        sb.append(AGENT_HEADER.formatted(i + 1, agentLabel));

        try {
          var node = JsonUtils.getObjectMapper().readTree(entry.getMessagesJson());
          var arr = node.isArray() ? node : node.get("messages");
          if (arr != null && arr.isArray()) {
            for (var msg : arr) {
              String type = msg.has("type") ? msg.get("type").asText() : "";
              if ("SYSTEM".equalsIgnoreCase(type)) {
                String text = extractMessageText(msg);
                if (text != null) {
                  sb.append(MSG_SYSTEM.formatted(truncate(text, 400)));
                }
                break;
              }
            }
            String lastAiText = null;
            for (var msg : arr) {
              String type = msg.has("type") ? msg.get("type").asText() : "";
              if ("AI".equalsIgnoreCase(type)) {
                String text = extractMessageText(msg);
                if (text != null && !text.isBlank()) {
                  lastAiText = text;
                }
              }
            }
            if (lastAiText != null) {
              sb.append(MSG_AI_RESPONSE.formatted(truncate(lastAiText, 600)));
            }
          }
        } catch (Exception e) {
          sb.append(MSG_DATA_UNAVAILABLE);
        }

        List<AgentConversationEntry.ToolExecution> tools = entry.getToolExecutions();
        if (tools != null && !tools.isEmpty()) {
          sb.append(MSG_TOOLS_PREFIX);
          tools.forEach(t -> sb.append(t.toolName()).append(" -> ")
              .append(truncate(t.resultText(), 120)).append(" | "));
          sb.append("\n");
        }

        List<AgentConversationEntry.GuardrailExecution> guardrails = entry.getGuardrailExecutions();
        if (guardrails != null && !guardrails.isEmpty()) {
          sb.append(MSG_GUARDRAILS_PREFIX);
          guardrails.forEach(g -> sb.append(g.guardrailName()).append(":").append(g.result()).append(" "));
          sb.append("\n");
        }
        sb.append("\n");
      }
    }

    sb.append(SECTION_END);
    return sb.toString();
  }

  private static String extractMessageText(com.fasterxml.jackson.databind.JsonNode msg) {
    if (msg.has("text")) {
      return msg.get("text").asText();
    }
    var contents = msg.get("contents");
    if (contents != null && contents.isArray()) {
      StringBuilder t = new StringBuilder();
      for (var c : contents) {
        if (c.has("text") && "TEXT".equalsIgnoreCase(c.has("type") ? c.get("type").asText() : "")) {
          t.append(c.get("text").asText());
        }
      }
      return t.length() > 0 ? t.toString() : null;
    }
    return null;
  }

  private static String truncate(String text, int maxLen) {
    if (text == null) return "";
    text = text.replaceAll("\\s+", " ").trim();
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
  }

  public static String generateReport(String caseId, List<AgentSummary> summaries) {
    if (summaries == null || summaries.isEmpty()) {
      return NO_DATA_MSG.formatted(caseId);
    }
    String caseName = resolveCaseName(caseId);
    return new AnalyticReportBuilder(caseId, caseName, summaries).render();
  }

  private static String resolveCaseName(String caseId) {
    try {
      var ivyCase = Ivy.wf().findCase(Long.parseLong(caseId));
      return ivyCase != null ? ivyCase.getName() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static AgentSummary summarize(AgentConversationEntry entry, List<ToolSummary> allToolSummaries,
      List<GuardrailSummary> allGuardrailSummaries, TokenUsageSummary tokenUsageSummary) {
    List<ToolExecution> tools = entry.getToolExecutions();
    int toolCallCount = tools != null ? tools.size() : 0;
    Set<String> entryToolNames = tools != null
        ? tools.stream().map(ToolExecution::toolName).collect(Collectors.toSet())
        : Collections.emptySet();
    List<ToolSummary> toolSummaries = allToolSummaries.stream()
        .filter(ts -> entryToolNames.contains(ts.getToolName()))
        .collect(Collectors.toList());

    List<ResponseMetadata> metadataList = parseMetadata(entry.getTokenUsageJson());
    int totalTokens = metadataList.stream().mapToInt(m -> m.totalTokens() != null ? m.totalTokens() : 0).sum();
    long durationMs = metadataList.stream().mapToLong(m -> m.durationMs() != null ? m.durationMs() : 0).sum();
    String finishReason = metadataList.isEmpty() ? null : metadataList.getLast().finishReason();
    String model = metadataList.isEmpty() ? null : metadataList.getLast().modelName();

    List<GuardrailExecution> guardrails = entry.getGuardrailExecutions();
    Set<String> entryGuardrailNames = guardrails != null
        ? guardrails.stream().map(GuardrailExecution::guardrailName).collect(Collectors.toSet())
        : Collections.emptySet();
    List<GuardrailSummary> guardrailSummaries = allGuardrailSummaries.stream()
        .filter(gs -> entryGuardrailNames.contains(gs.getGuardrailName()))
        .collect(Collectors.toList());

    return new AgentSummary(entry.getAgentId(), entry.getAgentName(), entry.getProcessName(),
        countMessages(entry.getMessagesJson()),
        toolCallCount, totalTokens, durationMs, finishReason, model, toolSummaries, guardrailSummaries, tokenUsageSummary,
        buildAnomalyReport(tools, toolSummaries, guardrailSummaries, totalTokens, durationMs, finishReason));
  }

  private static AnomalyReport buildAnomalyReport(List<ToolExecution> tools,
      List<ToolSummary> toolSummaries, List<GuardrailSummary> guardrailSummaries,
      int totalTokens, long durationMs, String finishReason) {
    List<String> issues = new ArrayList<>();

    if (durationMs > 30_000) {
      issues.add(ANOMALY_DURATION.formatted(durationMs));
    }
    if (totalTokens > 10_000) {
      issues.add(ANOMALY_TOKENS.formatted(totalTokens));
    }
    int toolCallCount = tools != null ? tools.size() : 0;
    if (toolCallCount > 10) {
      issues.add(ANOMALY_TOOL_CALLS.formatted(toolCallCount));
    }
    if (guardrailSummaries != null) {
      guardrailSummaries.stream()
          .filter(gs -> gs.getFatalCount() > 0)
          .forEach(gs -> issues.add(ANOMALY_FATAL_GUARDRAIL.formatted(gs.getGuardrailName(), gs.getFatalCount())));
    }
    if ("LENGTH".equalsIgnoreCase(finishReason)) {
      issues.add(ANOMALY_LENGTH);
    }
    if (toolSummaries != null) {
      toolSummaries.stream()
          .filter(ts -> ts.getCallCount() > 0 && (double) ts.getNullResultCount() / ts.getCallCount() > 0.5)
          .forEach(ts -> issues.add(ANOMALY_NULL_RESULT.formatted(ts.getToolName(), ts.getNullResultCount(), ts.getCallCount())));
    }
    if (tools != null) {
      tools.stream()
          .filter(t -> t.arguments() != null)
          .collect(Collectors.groupingBy(t -> t.toolName() + "::" + t.arguments(), Collectors.counting()))
          .entrySet().stream()
          .filter(e -> e.getValue() > 5)
          .forEach(e -> {
            String[] parts = e.getKey().split("::", 2);
            issues.add(ANOMALY_STUCK_LOOP.formatted(parts[0], e.getValue()));
          });
    }
    return new AnomalyReport(issues);
  }

  private static List<GuardrailSummary> buildGuardrailSummaries(List<AgentConversationEntry> entries) {
    List<GuardrailExecution> allGuardrails = entries.stream()
        .flatMap(e -> e.getGuardrailExecutions() != null ? e.getGuardrailExecutions().stream() : java.util.stream.Stream.empty())
        .collect(Collectors.toList());
    return allGuardrails.stream()
        .collect(Collectors.groupingBy(GuardrailExecution::guardrailName))
        .entrySet().stream()
        .map(eg -> {
          List<GuardrailExecution> group = eg.getValue();
          int passedCount = (int) group.stream().filter(g -> "PASSED".equalsIgnoreCase(g.result()) || "SUCCESS".equalsIgnoreCase(g.result())).count();
          int failedCount = (int) group.stream().filter(g -> "FAILED".equalsIgnoreCase(g.result())).count();
          int fatalCount  = (int) group.stream().filter(g -> "FATAL".equalsIgnoreCase(g.result())).count();
          double avgDurationMs = group.stream()
              .filter(g -> g.durationMs() != null)
              .mapToLong(GuardrailExecution::durationMs)
              .average().orElse(0);
          return new GuardrailSummary(eg.getKey(), passedCount, failedCount, fatalCount, avgDurationMs);
        })
        .collect(Collectors.toList());
  }

  private static TokenUsageSummary buildTokenUsageSummary(List<AgentConversationEntry> entries) {
    List<ResponseMetadata> allMetadata = entries.stream()
        .flatMap(e -> parseMetadata(e.getTokenUsageJson()).stream())
        .collect(Collectors.toList());
    if (allMetadata.isEmpty()) {
      return new TokenUsageSummary(0, 0, 0, 0, 0, 0, 0);
    }
    int totalInput  = allMetadata.stream().mapToInt(m -> m.inputTokens()  != null ? m.inputTokens()  : 0).sum();
    int totalOutput = allMetadata.stream().mapToInt(m -> m.outputTokens() != null ? m.outputTokens() : 0).sum();
    int totalAll    = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).sum();
    double avgInput  = allMetadata.stream().mapToInt(m -> m.inputTokens()  != null ? m.inputTokens()  : 0).average().orElse(0);
    double avgOutput = allMetadata.stream().mapToInt(m -> m.outputTokens() != null ? m.outputTokens() : 0).average().orElse(0);
    double avgTotal  = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).average().orElse(0);
    int maxSingle    = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).max().orElse(0);
    return new TokenUsageSummary(totalInput, totalOutput, totalAll, avgInput, avgOutput, avgTotal, maxSingle);
  }

  private static List<ToolSummary> buildToolSummaries(List<AgentConversationEntry> entries) {
    List<ToolExecution> allTools = entries.stream()
        .flatMap(e -> e.getToolExecutions() != null ? e.getToolExecutions().stream() : java.util.stream.Stream.empty())
        .collect(Collectors.toList());
    return allTools.stream()
        .collect(Collectors.groupingBy(ToolExecution::toolName))
        .entrySet().stream()
        .map(eg -> {
          List<ToolExecution> group = eg.getValue();
          int callCount = group.size();
          int nullResultCount = (int) group.stream().filter(t -> StringUtils.isBlank(t.resultText())).count();
          int errorCount = (int) group.stream()
              .filter(t -> t.resultText() != null && t.resultText().toLowerCase().contains("error")).count();
          String sampleArguments = group.stream()
              .map(ToolExecution::arguments)
              .filter(a -> !StringUtils.isBlank(a))
              .findFirst().orElse(null);
          return new ToolSummary(eg.getKey(), callCount, nullResultCount, errorCount, sampleArguments);
        })
        .collect(Collectors.toList());
  }

  private static int countMessages(String messagesJson) {
    if (messagesJson == null || messagesJson.isBlank()) {
      return 0;
    }
    try {
      var node = JsonUtils.getObjectMapper().readTree(messagesJson);
      if (node.isArray()) {
        return node.size();
      }
      var arr = node.get("messages");
      return arr != null && arr.isArray() ? arr.size() : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  private static List<ResponseMetadata> parseMetadata(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return JsonUtils.getObjectMapper().readValue(json, new TypeReference<List<ResponseMetadata>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
