package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

import java.util.List;

public class AnomalyReport {

  private List<String> issues;

  public AnomalyReport() {}

  public AnomalyReport(List<String> issues) {
    this.issues = issues;
  }

  public List<String> getIssues() { return issues; }
  public void setIssues(List<String> issues) { this.issues = issues; }

  public boolean hasIssues() { return issues != null && !issues.isEmpty(); }
}
