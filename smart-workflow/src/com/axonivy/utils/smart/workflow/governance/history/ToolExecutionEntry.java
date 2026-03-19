package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;

public class ToolExecutionEntry {

  private String agentId;
  private String caseUuid;
  private String taskUuid;
  private String toolName;
  private String arguments;
  private String resultText;
  private LocalDateTime executedAt;

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getTaskUuid() { return taskUuid; }
  public void setTaskUuid(String taskUuid) { this.taskUuid = taskUuid; }

  public String getToolName() { return toolName; }
  public void setToolName(String toolName) { this.toolName = toolName; }

  public String getArguments() { return arguments; }
  public void setArguments(String arguments) { this.arguments = arguments; }

  public String getResultText() { return resultText; }
  public void setResultText(String resultText) { this.resultText = resultText; }

  public LocalDateTime getExecutedAt() { return executedAt; }
  public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
