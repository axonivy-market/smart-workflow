package com.axonivy.utils.smart.workflow.governance.ui.entity;

public enum HistoryNodeType {
  ROOT, CASE, TASK, AGENT;

  public String value() {
    return name().toLowerCase();
  }
}
