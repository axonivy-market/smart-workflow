package com.axonivy.utils.smart.workflow.governance.history.recorder.internal;

import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.governance.history.recorder.ToolExecutionRecorder;
import com.axonivy.utils.smart.workflow.governance.history.storage.ToolExecutionStorage;
import com.axonivy.utils.smart.workflow.utils.DateParsingUtils;

public class ToolExecutionRepository implements ToolExecutionRecorder {

  private final String caseUuid;
  private final String taskUuid;
  private final String agentId;
  private final ToolExecutionStorage storage;

  public ToolExecutionRepository(String caseUuid, String taskUuid, String agentId,
      ToolExecutionStorage storage) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
    this.agentId = agentId;
    this.storage = storage;
  }

  @Override
  public void record(String toolName, String arguments, String resultText) {
    var entry = new ToolExecutionEntry();
    entry.setCaseUuid(caseUuid);
    entry.setTaskUuid(taskUuid);
    entry.setAgentId(agentId);
    entry.setToolName(toolName);
    entry.setArguments(arguments);
    entry.setResultText(resultText);
    entry.setExecutedAt(DateParsingUtils.now());
    storage.save(entry);
  }
}
