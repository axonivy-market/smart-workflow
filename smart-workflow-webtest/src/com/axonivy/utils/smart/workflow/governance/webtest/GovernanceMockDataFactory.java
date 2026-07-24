package com.axonivy.utils.smart.workflow.governance.webtest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

public class GovernanceMockDataFactory {

  public static final int EXPECTED_COUNT = 8;

  public interface CaseName {
    String CASE_1 = "webtest-case-001-agent-pipeline";
    String CASE_2 = "webtest-case-002-agent-pipeline";
    String CASE_3 = "webtest-case-003-agent-pipeline";
    String CASE_4 = "webtest-case-004-agent-pipeline";
  }

  interface TaskName {
    String TASK_1A = "webtest-task-001a-image";
    String TASK_1B = "webtest-task-001b-analysis";
    String TASK_2A = "webtest-task-002a-image";
    String TASK_3A = "webtest-task-003a-image";
    String TASK_4A = "webtest-task-004a-image";
  }

  interface AgentId {
    String OCR     = "d4ea0ad3-c023-35cf-af9d-4a8f59186649";
    String HEADER  = "1b3556aa-5c12-3444-bbd4-13015f700a7f";
    String ITEMS   = "5cefeb73-65af-3f21-8610-b6c40c286da0";
    String COMPLY  = "1a2626f0-2460-30dd-90aa-9a33b75e6700";
    String AMOUNTS = "0a443821-1732-32d2-8809-e40988fe57d0";
  }

  interface AgentName {
    String OCR     = "Extract Invoice Content from Image";
    String HEADER  = "Extract Header Info Agent";
    String ITEMS   = "Extract Line Items Agent";
    String COMPLY  = "Assess Compliance Agent";
    String AMOUNTS = "Validate Amounts Agent";
  }

  interface Meta {
    String PROCESS = "Agent Pipeline Demo";
    String MODEL   = "gpt-4.1-mini-2025-04-14";
  }

  enum TestEntry {
    CASE1_TASK1A_OCR(CaseName.CASE_1, TaskName.TASK_1A, AgentId.OCR, AgentName.OCR, 0,
        "You are an invoice OCR specialist.",
        "Extract all text content from this invoice image.",
        "Invoice Number: INV-0001-0001 Invoice Date: January 15, 2024",
        1248, 364),

    CASE1_TASK1B_HEADER(CaseName.CASE_1, TaskName.TASK_1B, AgentId.HEADER, AgentName.HEADER, 0,
        "You are an invoice header extraction specialist.",
        "Extract header information from this invoice content.",
        """
        {"invoiceNumber":"INV-0001-0001","invoiceDate":"January 15, 2024"}""",
        433, 113),

    CASE1_TASK1B_ITEMS(CaseName.CASE_1, TaskName.TASK_1B, AgentId.ITEMS, AgentName.ITEMS, 0,
        "You are an invoice line item extraction specialist.",
        "Extract all line items from this invoice content.",
        """
        [{"description":"Software License","quantity":1,"unitPrice":12500.00}]""",
        438, 257),

    CASE1_TASK1B_COMPLY(CaseName.CASE_1, TaskName.TASK_1B, AgentId.COMPLY, AgentName.COMPLY, 0,
        "You are an invoice compliance specialist.",
        "Assess the compliance of this invoice.",
        "Overall compliance rating: 7/10",
        472, 531),

    CASE1_TASK1B_AMOUNTS(CaseName.CASE_1, TaskName.TASK_1B, AgentId.AMOUNTS, AgentName.AMOUNTS, 0,
        "You are an invoice amount validation specialist.",
        "Validate the amounts in these invoice line items.",
        "All line items have correct calculations.",
        346, 153),

    CASE2_TASK2A_OCR(CaseName.CASE_2, TaskName.TASK_2A, AgentId.OCR, AgentName.OCR, 0,
        "You are an invoice OCR specialist.",
        "Extract all text content from this second invoice.",
        "Invoice Number: INV-0002-0001 Invoice Date: March 10, 2024",
        900, 200),

    CASE3_TASK3A_OCR(CaseName.CASE_3, TaskName.TASK_3A, AgentId.OCR, AgentName.OCR, 10,
        "You are an invoice OCR specialist.",
        "Extract all text content from this invoice.",
        "Invoice Number: INV-0003-0001 Invoice Date: June 20, 2024",
        950, 210),

    CASE4_TASK4A_OCR(CaseName.CASE_4, TaskName.TASK_4A, AgentId.OCR, AgentName.OCR, 40,
        "You are an invoice OCR specialist.",
        "Extract all text content from this invoice.",
        "Invoice Number: INV-0004-0001 Invoice Date: May 5, 2024",
        880, 190);

    final String caseName;
    final String taskName;
    final String agentId;
    final String agentName;
    final long daysAgo;
    final String systemMsg;
    final String userMsg;
    final String aiMsg;
    final int inputTokens;
    final int outputTokens;

    TestEntry(String caseName, String taskName, String agentId, String agentName, long daysAgo,
        String systemMsg, String userMsg, String aiMsg, int inputTokens, int outputTokens) {
      this.caseName = caseName;
      this.taskName = taskName;
      this.agentId = agentId;
      this.agentName = agentName;
      this.daysAgo = daysAgo;
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

    Map<String, ICase> caseCache = new HashMap<>();
    for (TestEntry e : TestEntry.values()) {
      caseCache.computeIfAbsent(e.caseName, GovernanceMockDataFactory::findCaseByName);
    }

    for (TestEntry e : TestEntry.values()) {
      ICase ivyCase = caseCache.get(e.caseName);
      String caseUuid = ivyCase != null ? ivyCase.uuid() : e.caseName;
      ITask ivyTask = ivyCase != null ? findTaskByName(ivyCase, e.taskName) : null;
      String taskUuid = ivyTask != null ? ivyTask.uuid() : e.taskName;
      saveEntry(caseUuid, taskUuid, e.agentId, e.agentName, Meta.PROCESS,
          messages(e.systemMsg, e.userMsg, e.aiMsg),
          tokens(e.inputTokens, e.outputTokens),
          now.minusDays(e.daysAgo));
    }
  }

  public static long countCreatedEntries() {
    return Ivy.repo().search(AgentConversationEntry.class)
        .execute().getAll().size();
  }

  public static void deleteAll() {
    Ivy.repo().search(AgentConversationEntry.class)
        .execute().getAll()
        .forEach(e -> Ivy.repo().delete(e));
  }

  private static ICase findCaseByName(String name) {
    try {
      return Sudo.get(() -> CaseQuery.create().where().name().isEqual(name)
          .executor().firstResult());
    } catch (Exception e) {
      return null;
    }
  }

  private static ITask findTaskByName(ICase ivyCase, String taskName) {
    try {
      long caseId = ivyCase.getId();
      return Sudo.get(() -> TaskQuery.create()
          .where().caseId().isEqual(caseId)
          .and().name().isEqual(taskName)
          .executor().firstResult());
    } catch (Exception e) {
      return null;
    }
  }

  private static void saveEntry(String caseUuid, String taskUuid, String agentId,
      String agentName, String processName, String messagesJson, String tokenUsageJson,
      LocalDateTime lastUpdated) {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid(caseUuid);
    entry.setTaskUuid(taskUuid);
    entry.setAgentId(agentId);
    entry.setAgentName(agentName);
    entry.setProcessName(processName);
    entry.setMessagesJson(messagesJson);
    entry.setTokenUsageJson(tokenUsageJson);
    entry.setLastUpdated(lastUpdated.toString());
    Ivy.repo().save(entry);
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
        "durationMs":1000,"aiServiceMethod":"chat","toolNames":[]}]"""
        .formatted(in, out, in + out, Meta.MODEL);
  }

  private static String esc(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
