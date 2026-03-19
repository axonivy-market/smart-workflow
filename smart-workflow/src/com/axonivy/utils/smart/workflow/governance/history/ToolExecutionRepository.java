package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;

import dev.langchain4j.observability.api.event.ToolExecutedEvent;

public class ToolExecutionRepository {

  /** Overrides the default Ivy.repo() storage in tests. Null in production. */
  static ToolExecutionStorage testStorage = null;

  private final String caseUuid;
  private final String taskUuid;
  private final String agentId;
  private final ToolExecutionStorage storage;

  public ToolExecutionRepository(String caseUuid, String taskUuid, String agentId) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
    this.agentId = agentId;
    this.storage = testStorage != null ? testStorage : new IvyRepoToolExecutionStorage();
  }

  public void record(ToolExecutedEvent event) {
    var entry = new ToolExecutionEntry();
    entry.setCaseUuid(caseUuid);
    entry.setTaskUuid(taskUuid);
    entry.setAgentId(agentId);
    entry.setToolName(event.request().name());
    entry.setArguments(event.request().arguments());
    entry.setResultText(event.resultText());
    entry.setExecutedAt(LocalDateTime.now());
    storage.save(entry);
  }
}
