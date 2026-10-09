package com.axonivy.utils.smart.workflow.governance.webtest;

import java.time.LocalDateTime;
import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.EfficiencyFinding;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.Recommendation;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.ReliabilityConcerns;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.RiskAssessment;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.RiskEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.ToolUsagePatterns;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReportEntry;

public class ConversationsMockDataFactory {

  public static final int EXPECTED_COUNT = 3;
  public static final String CASE_UUID = "webtest-conv-case-001";

  interface TaskName {
    String TASK_1A = "webtest-conv-task-001a";
    String TASK_1B = "webtest-conv-task-001b";
  }

  interface AgentId {
    String OCR      = "d4ea0ad3-c023-35cf-af9d-4a8f59186649";
    String HEADER   = "1b3556aa-5c12-3444-bbd4-13015f700a7f";
    String ANALYZER = "a01c6042-fa87-3133-abba-8e5cf87e8779";
  }

  public interface AgentName {
    String OCR      = "Extract Invoice Content from Image";
    String HEADER   = "Extract Header Info Agent";
    String ANALYZER = "Invoice Analyzer Agent";
  }

  public interface ToolName {
    String EXTRACT_HEADER     = "extractHeaderInfo";
    String EXTRACT_ITEMS      = "extractLineItems";
    String ASSESS_COMPLIANCE  = "assessCompliance";
    String VALIDATE_AMOUNTS   = "validateAmounts";
  }

  /** Expected content of the mocked AI governance report, asserted by the web test. */
  public interface AiReport {
    String SUMMARY = "All agents performed reliably with excellent grades except the Invoice Analyzer Agent, "
        + "which showed a significant performance anomaly due to long processing time despite accurate and complete analysis.";

    String EFFICIENCY_OBSERVATION = "Duration significantly exceeded expected threshold (69 seconds vs average ~16 seconds).";
    String EFFICIENCY_SUGGESTION  = "Optimize processing logic to reduce duration and token usage.";

    String ANOMALY               = "Invoice Analyzer Agent duration exceeded 30 seconds (69358ms).";
    String RELIABILITY_CONCLUSION = "No errors or guardrail violations detected; overall reliable but performance anomaly noted in Invoice Analyzer Agent.";

    String TOOL_OBSERVATION = "All tool calls succeeded with 100% success rate.";
    String TOOL_INSIGHT     = "Tool usage is effective and reliable, but centralized in one agent which may contribute to its longer processing time.";
    int    TOOL_CALL_COUNT  = 4;
    int    TOOLS_USED_COUNT = 4;

    String RECOMMENDATION_PERFORMANCE = "Optimize Invoice Analyzer Agent Performance";
    String RECOMMENDATION_DATA_QUALITY = "Improve Data Quality and Compliance";

    /** operational=Moderate, compliance/cost/reliability=Low -> sorted Moderate first. */
    int MODERATE_RISK_COUNT = 1;
    int LOW_RISK_COUNT      = 3;
    int HIGH_RISK_COUNT     = 0;
  }

  public interface Meta {
    String PROCESS = "Agent Pipeline Demo";
    String MODEL   = "gpt-4.1-mini-2025-04-14";
  }

  enum TestEntry {
    TASK1A_OCR(TaskName.TASK_1A, AgentId.OCR, AgentName.OCR, false,
        "You are an invoice OCR specialist.",
        "Extract all text content from this invoice image.",
        "Invoice Number: INV-0001-0001 Invoice Date: January 15, 2024",
        1248, 364),

    TASK1B_HEADER(TaskName.TASK_1B, AgentId.HEADER, AgentName.HEADER, false,
        "You are an invoice header extraction specialist.",
        "Extract header information from this invoice content.",
        "{\"invoiceNumber\":\"INV-0001-0001\",\"invoiceDate\":\"January 15, 2024\"}",
        433, 113),

    TASK1B_ANALYZER(TaskName.TASK_1B, AgentId.ANALYZER, AgentName.ANALYZER, true,
        "You are a comprehensive invoice analysis specialist.",
        "Analyze this invoice document.",
        "Invoice Analysis Report: All calculations are correct. Compliance rating: 7/10.",
        645, 549);

    final String taskUuid;
    final String agentId;
    final String agentName;
    final boolean hasTools;
    final String systemMsg;
    final String userMsg;
    final String aiMsg;
    final int inputTokens;
    final int outputTokens;

    TestEntry(String taskUuid, String agentId, String agentName, boolean hasTools,
        String systemMsg, String userMsg, String aiMsg, int inputTokens, int outputTokens) {
      this.taskUuid = taskUuid;
      this.agentId = agentId;
      this.agentName = agentName;
      this.hasTools = hasTools;
      this.systemMsg = systemMsg;
      this.userMsg = userMsg;
      this.aiMsg = aiMsg;
      this.inputTokens = inputTokens;
      this.outputTokens = outputTokens;
    }
  }

  public static void createAll() {
    deleteAll();
    LocalDateTime now = LocalDateTime.now();
    int i = 0;
    for (TestEntry e : TestEntry.values()) {
      saveEntry(CASE_UUID, e.taskUuid, e.agentId, e.agentName, Meta.PROCESS,
          messages(e.systemMsg, e.userMsg, e.aiMsg),
          tokens(e.inputTokens, e.outputTokens),
          now.plusSeconds(i++),
          e.hasTools ? toolExecutions() : null);
    }
    saveAiReport();
  }

  public static long countCreatedEntries() {
    return Ivy.repo().search(AgentConversationEntry.class)
        .execute().getAll().stream()
        .filter(e -> CASE_UUID.equals(e.getCaseUuid()))
        .count();
  }

  public static void deleteAll() {
    Ivy.repo().search(AgentConversationEntry.class)
        .execute().getAll().stream()
        .filter(e -> CASE_UUID.equals(e.getCaseUuid()))
        .forEach(e -> Ivy.repo().delete(e));
    Ivy.repo().search(AiGovernanceReportEntry.class)
        .execute().getAll().stream()
        .filter(e -> CASE_UUID.equals(e.getCaseUuid()))
        .forEach(e -> Ivy.repo().delete(e));
  }

  /**
   * Persists a deterministic AI governance report so the "AI Recommendation" tab renders the
   * report sections instead of the generate button - clicking that button would invoke a real LLM.
   */
  private static void saveAiReport() {
    var report = new AiGovernanceReport();
    report.setSummary(AiReport.SUMMARY);
    report.setEfficiencyOpportunities(List.of(new EfficiencyFinding(
        AgentName.ANALYZER,
        List.of(AiReport.EFFICIENCY_OBSERVATION,
            "High token usage and time share (57% tokens, 69% time) indicating potential inefficiency."),
        List.of(AiReport.EFFICIENCY_SUGGESTION,
            "Consider splitting tasks or parallelizing to improve throughput."))));
    report.setReliabilityConcerns(new ReliabilityConcerns(
        List.of(AiReport.ANOMALY), List.of(), AiReport.RELIABILITY_CONCLUSION));
    report.setToolUsagePatterns(new ToolUsagePatterns(
        List.of(ToolName.EXTRACT_HEADER, ToolName.EXTRACT_ITEMS,
            ToolName.ASSESS_COMPLIANCE, ToolName.VALIDATE_AMOUNTS),
        AiReport.TOOL_CALL_COUNT,
        List.of(AiReport.TOOL_OBSERVATION,
            "Tools were used exclusively by the Invoice Analyzer Agent.",
            "Tools returned consistent and valid outputs."),
        AiReport.TOOL_INSIGHT));
    report.setRiskAssessment(new RiskAssessment(
        new RiskEntry("Moderate", "The Invoice Analyzer Agent's long duration may impact operational throughput and latency."),
        new RiskEntry("Low", "Compliance issues are minor and mostly relate to placeholder data rather than missing critical fields."),
        new RiskEntry("Low", "Token usage and costs are within reasonable limits; no excessive consumption detected."),
        new RiskEntry("Low", "No errors or failures detected; system is reliable but performance bottleneck exists.")));
    report.setRecommendations(List.of(
        new Recommendation(AiReport.RECOMMENDATION_PERFORMANCE, List.of(
            "Review and refactor the Invoice Analyzer Agent to reduce processing time.",
            "Consider distributing tool calls across multiple agents or parallelizing tasks.",
            "Monitor agent performance post-optimization to ensure improvements.")),
        new Recommendation(AiReport.RECOMMENDATION_DATA_QUALITY, List.of(
            "Replace placeholder VAT numbers and IBANs with valid data.",
            "Add vendor contact phone number and company registration number if required by jurisdiction.",
            "Enhance formatting of invoice number and date section for clarity."))));

    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid(CASE_UUID);
    entry.setGeneratedAt(LocalDateTime.now().toString());
    entry.setReport(report);
    Ivy.repo().save(entry);
  }

  private static void saveEntry(String caseUuid, String taskUuid, String agentId,
      String agentName, String processName, String messagesJson, String tokenUsageJson,
      LocalDateTime lastUpdated, List<ToolExecution> toolExecutions) {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid(caseUuid);
    entry.setTaskUuid(taskUuid);
    entry.setAgentId(agentId);
    entry.setAgentName(agentName);
    entry.setProcessName(processName);
    entry.setMessagesJson(messagesJson);
    entry.setTokenUsageJson(tokenUsageJson);
    entry.setLastUpdated(lastUpdated.toString());
    entry.setToolExecutions(toolExecutions);
    Ivy.repo().save(entry);
  }

  private static List<ToolExecution> toolExecutions() {
    String ts = LocalDateTime.now().toString();
    return List.of(
        new ToolExecution(ToolName.EXTRACT_HEADER,
            "{\"invoiceContent\": \"Invoice Number: INV-0001-0001\"}",
            "{\"invoiceNumber\": \"INV-0001-0001\", \"invoiceDate\": \"January 15, 2024\"}",
            ts),
        new ToolExecution(ToolName.EXTRACT_ITEMS,
            "{\"invoiceContent\": \"Invoice Number: INV-0001-0001\"}",
            "[{\"description\": \"Software License\", \"lineTotal\": 12500.00}]",
            ts));
  }

  private static String messages(String system, String user, String ai) {
    return """
        [{"text":"%s","type":"SYSTEM"},\
        {"contents":[{"text":"%s","type":"TEXT"}],"type":"USER"},\
        {"text":"%s","toolExecutionRequests":[],"attributes":{},"type":"AI"}]"""
        .formatted(esc(system), esc(user), esc(ai));
  }

  private static String tokens(int in, int out) {
    return """
        [{"inputTokens":%d,"outputTokens":%d,"totalTokens":%d,\
        "finishReason":"STOP","modelName":"%s",\
        "durationMs":5000,"aiServiceMethod":"chat","toolNames":[]}]"""
        .formatted(in, out, in + out, Meta.MODEL);
  }

  private static String esc(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
