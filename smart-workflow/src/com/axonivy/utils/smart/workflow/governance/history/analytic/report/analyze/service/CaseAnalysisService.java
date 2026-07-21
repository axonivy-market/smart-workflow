package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.entity.CaseAnalysisResult;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service.AgentSummaryService;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ICase;

public class CaseAnalysisService {

  private static final String NO_DATA_MSG = "No agent data available for case %s.";
  private static final String SUMMARIES_LOG_MSG = "Summaries for caseUuid {0}: {1}";

  interface ANALYZE_AGENT_HISTORY {
    String SIGNATURE = "analyzeAgentHistoryByCase(String, ch.ivyteam.ivy.workflow.ICase, String)";
    String PARAM_CASE_UUID = "caseUuid";
    String PARAM_CASE = "iCase";
    String PARAM_PROMPT = "prompt";
    String RESULT_REPORT = "governanceReport";
  }

  public static List<AgentSummary> buildSummaries(List<AgentConversationEntry> entries) {
    List<ToolSummary> allToolSummaries = AgentSummaryService.summarizeTools(entries);
    List<GuardrailSummary> allGuardrailSummaries = AgentSummaryService.summarizeGuardrails(entries);
    TokenUsageSummary tokenUsageSummary = AgentSummaryService.summarizeTokenUsage(entries);
    return entries.stream()
        .map(e -> AgentSummaryService.summarizeAgent(e, allToolSummaries, allGuardrailSummaries, tokenUsageSummary))
        .toList();
  }

  public static CaseAnalysisResult analyze(String caseUuid, ICase ivyCase, List<AgentConversationEntry> entries) {
    List<AgentSummary> summaries = buildSummaries(entries);

    String prompt = buildAiPrompt(caseUuid, summaries, entries);
    Map<String, Object> result = Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.SECURITY_CONTEXT)
          .setSignature(ANALYZE_AGENT_HISTORY.SIGNATURE)
          .toFilter();
      var startList = SubProcessCallStartEvent.find(filter);
      if (!startList.isEmpty()) {
        return startList.get(0)
            .withParam(ANALYZE_AGENT_HISTORY.PARAM_CASE_UUID, caseUuid)
            .withParam(ANALYZE_AGENT_HISTORY.PARAM_CASE, ivyCase)
            .withParam(ANALYZE_AGENT_HISTORY.PARAM_PROMPT, prompt)
            .call().asMap();
      }
      return Map.of();
    });
    AiGovernanceReport governanceReport = (AiGovernanceReport) result.get(ANALYZE_AGENT_HISTORY.RESULT_REPORT);

    Ivy.log().info(SUMMARIES_LOG_MSG, caseUuid, summaries);
    return new CaseAnalysisResult(summaries, governanceReport);
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
        .filter(AgentSummary::hasAnomalyIssues)
        .count();
  }

  public static String toJson(List<AgentSummary> summaries) {
    try {
      return JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(summaries);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }

  public static String buildAiPrompt(String caseId, List<AgentSummary> summaries,
      List<AgentConversationEntry> entries) {
    if (summaries == null || summaries.isEmpty()) {
      return NO_DATA_MSG.formatted(caseId);
    }
    return PromptBuilderService.build(caseId, resolveCaseName(caseId), summaries, entries);
  }

  private static String resolveCaseName(String caseId) {
    long id;
    try {
      id = Long.parseLong(caseId);
    } catch (NumberFormatException e) {
      return null;
    }
    var ivyCase = Ivy.wf().findCase(id);
    return ivyCase != null ? ivyCase.getName() : null;
  }
}
