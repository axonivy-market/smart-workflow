package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

public class GuardrailSummary {

  private String guardrailName;
  private int passedCount;
  private int failedCount;
  private int fatalCount;
  private double avgDurationMs;

  public GuardrailSummary() {}

  public GuardrailSummary(String guardrailName, int passedCount, int failedCount, int fatalCount, double avgDurationMs) {
    this.guardrailName = guardrailName;
    this.passedCount = passedCount;
    this.failedCount = failedCount;
    this.fatalCount = fatalCount;
    this.avgDurationMs = avgDurationMs;
  }

  public String getGuardrailName() { return guardrailName; }
  public void setGuardrailName(String guardrailName) { this.guardrailName = guardrailName; }

  public int getPassedCount() { return passedCount; }
  public void setPassedCount(int passedCount) { this.passedCount = passedCount; }

  public int getFailedCount() { return failedCount; }
  public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

  public int getFatalCount() { return fatalCount; }
  public void setFatalCount(int fatalCount) { this.fatalCount = fatalCount; }

  public double getAvgDurationMs() { return avgDurationMs; }
  public void setAvgDurationMs(double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
}
