package com.axonivy.utils.smart.workflow.governance.webtest;

import java.time.LocalDateTime;
import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

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
    String EXTRACT_HEADER = "extractHeaderInfo";
    String EXTRACT_ITEMS  = "extractLineItems";
  }

  interface Meta {
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
    if (toolExecutions != null) {
      entry.setToolExecutions(toolExecutions);
    }
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
