package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

public record ToolStats(
    String name,
    int calls,
    int successes,
    int nulls,
    int errors,
    int successRate,
    String grade) {

  public String getName() { return name; }
  public int getCalls() { return calls; }
  public int getSuccesses() { return successes; }
  public int getNulls() { return nulls; }
  public int getErrors() { return errors; }
  public int getSuccessRate() { return successRate; }
  public String getGrade() { return grade; }
}
