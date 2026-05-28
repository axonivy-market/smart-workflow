package com.axonivy.utils.smart.workflow.governance.history.service;

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

public class HistoryAnalysisService {

  private static final IvyRepoHistoryStorage STORAGE = new IvyRepoHistoryStorage();

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
      return "No agent data available for case " + caseId + ".";
    }

    String caseName = resolveCaseName(caseId);

    StringBuilder sb = new StringBuilder();

    // Section 1: Structured metrics report (template report provides baseline context)
    sb.append("=== STRUCTURED METRICS REPORT ===\n\n");
    sb.append(new ReportRenderer(caseId, caseName, summaries).render());
    sb.append("\n\n");

    // Section 2: Condensed conversation excerpts per agent
    sb.append("=== RAW CONVERSATION EXCERPTS ===\n\n");
    sb.append("(System prompt + final AI response per agent, truncated for brevity)\n\n");

    if (entries != null) {
      for (int i = 0; i < entries.size(); i++) {
        AgentConversationEntry entry = entries.get(i);
        String agentLabel = (entry.getAgentName() != null && !entry.getAgentName().isBlank())
            ? entry.getAgentName() : entry.getAgentId();
        sb.append("--- Agent ").append(i + 1).append(": ").append(agentLabel).append(" ---\n");

        // Parse messages and extract system + last AI message
        try {
          var node = JsonUtils.getObjectMapper().readTree(entry.getMessagesJson());
          var arr = node.isArray() ? node : node.get("messages");
          if (arr != null && arr.isArray()) {
            // System message
            for (var msg : arr) {
              String type = msg.has("type") ? msg.get("type").asText() : "";
              if ("SYSTEM".equalsIgnoreCase(type)) {
                String text = extractMessageText(msg);
                if (text != null) {
                  sb.append("  [SYSTEM] ").append(truncate(text, 400)).append("\n");
                }
                break;
              }
            }
            // Last AI message
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
              sb.append("  [FINAL AI RESPONSE] ").append(truncate(lastAiText, 600)).append("\n");
            }
          }
        } catch (Exception e) {
          sb.append("  (conversation data unavailable)\n");
        }

        // Tool execution results (brief)
        List<AgentConversationEntry.ToolExecution> tools = entry.getToolExecutions();
        if (tools != null && !tools.isEmpty()) {
          sb.append("  [TOOLS] ");
          tools.forEach(t -> sb.append(t.toolName()).append(" -> ")
              .append(truncate(t.resultText(), 120)).append(" | "));
          sb.append("\n");
        }

        // Guardrail outcomes
        List<AgentConversationEntry.GuardrailExecution> guardrails = entry.getGuardrailExecutions();
        if (guardrails != null && !guardrails.isEmpty()) {
          sb.append("  [GUARDRAILS] ");
          guardrails.forEach(g -> sb.append(g.guardrailName()).append(":").append(g.result()).append(" "));
          sb.append("\n");
        }
        sb.append("\n");
      }
    }

    sb.append("=== END OF DATA ===\n");
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
      return "No agent data available for case " + caseId + ".";
    }
    String caseName = resolveCaseName(caseId);
    return new ReportRenderer(caseId, caseName, summaries).render();
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
      issues.add("Duration exceeded 30s (" + durationMs + "ms)");
    }
    if (totalTokens > 10_000) {
      issues.add("Tokens exceeded 10,000 per conversation (" + totalTokens + ")");
    }
    int toolCallCount = tools != null ? tools.size() : 0;
    if (toolCallCount > 10) {
      issues.add("More than 10 tool calls (" + toolCallCount + ") — possible loop");
    }
    if (guardrailSummaries != null) {
      guardrailSummaries.stream()
          .filter(gs -> gs.getFatalCount() > 0)
          .forEach(gs -> issues.add("FATAL guardrail result on '" + gs.getGuardrailName() + "' (" + gs.getFatalCount() + " time(s))"));
    }
    if ("LENGTH".equalsIgnoreCase(finishReason)) {
      issues.add("Finish reason is LENGTH — output was truncated");
    }
    if (toolSummaries != null) {
      toolSummaries.stream()
          .filter(ts -> ts.getCallCount() > 0 && (double) ts.getNullResultCount() / ts.getCallCount() > 0.5)
          .forEach(ts -> issues.add("Tool '" + ts.getToolName() + "' returned null/empty in "
              + ts.getNullResultCount() + "/" + ts.getCallCount() + " calls (>50%)"));
    }
    if (tools != null) {
      tools.stream()
          .filter(t -> t.arguments() != null)
          .collect(Collectors.groupingBy(t -> t.toolName() + "::" + t.arguments(), Collectors.counting()))
          .entrySet().stream()
          .filter(e -> e.getValue() > 5)
          .forEach(e -> {
            String[] parts = e.getKey().split("::", 2);
            issues.add("Tool '" + parts[0] + "' called " + e.getValue()
                + " times with identical arguments — possible stuck loop");
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
