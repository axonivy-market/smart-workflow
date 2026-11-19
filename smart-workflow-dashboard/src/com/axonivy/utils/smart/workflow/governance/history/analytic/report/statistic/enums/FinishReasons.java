package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums;

public enum FinishReasons {
  STOP, LENGTH;

  public boolean matches(String reason) {
    return name().equalsIgnoreCase(reason);
  }

  public static boolean isUnexpected(String reason) {
    return reason != null && !STOP.matches(reason) && !LENGTH.matches(reason);
  }
}
