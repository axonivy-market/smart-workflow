package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

import java.util.Objects;

public record AnomalyIssue(AnomalyType type, String message) {

  public AnomalyIssue {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");
  }
}
