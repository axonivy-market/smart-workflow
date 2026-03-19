package com.axonivy.utils.smart.workflow.governance.listener;

import com.axonivy.utils.smart.workflow.governance.history.ToolExecutionRepository;

import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;

public class ToolExecutionHistoryListener implements ToolExecutedEventListener {

  private final String caseUuid;
  private final String taskUuid;
  private ToolExecutionRepository repository;

  public ToolExecutionHistoryListener(String caseUuid, String taskUuid) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
  }

  public ToolExecutionHistoryListener(ToolExecutionRepository repository) {
    this.caseUuid = null;
    this.taskUuid = null;
    this.repository = repository;
  }

  @Override
  public void onEvent(ToolExecutedEvent event) {
    if (repository == null) {
      repository = new ToolExecutionRepository(caseUuid, taskUuid,
          event.invocationContext().invocationId().toString());
    }
    repository.record(event);
  }
}
