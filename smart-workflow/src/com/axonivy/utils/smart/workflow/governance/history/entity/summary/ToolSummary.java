package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

public class ToolSummary {

  private String toolName;
  private int callCount;
  private int nullResultCount;
  private int errorCount;
  private String sampleArguments;

  public ToolSummary() {}

  public ToolSummary(String toolName, int callCount, int nullResultCount, int errorCount, String sampleArguments) {
    this.toolName = toolName;
    this.callCount = callCount;
    this.nullResultCount = nullResultCount;
    this.errorCount = errorCount;
    this.sampleArguments = sampleArguments;
  }

  public String getToolName() { return toolName; }
  public void setToolName(String toolName) { this.toolName = toolName; }

  public int getCallCount() { return callCount; }
  public void setCallCount(int callCount) { this.callCount = callCount; }

  public int getNullResultCount() { return nullResultCount; }
  public void setNullResultCount(int nullResultCount) { this.nullResultCount = nullResultCount; }

  public int getErrorCount() { return errorCount; }
  public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

  public String getSampleArguments() { return sampleArguments; }
  public void setSampleArguments(String sampleArguments) { this.sampleArguments = sampleArguments; }

  @Override
  public String toString() {
    return toolName + "(calls=" + callCount + ", nulls=" + nullResultCount + ", errors=" + errorCount + ")";
  }
}
