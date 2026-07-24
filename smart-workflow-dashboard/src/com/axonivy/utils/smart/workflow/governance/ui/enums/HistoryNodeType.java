package com.axonivy.utils.smart.workflow.governance.ui.enums;

public enum HistoryNodeType {
  ROOT, CASE, TASK, AGENT;

  public String value() {
    return name().toLowerCase();
  }
}
