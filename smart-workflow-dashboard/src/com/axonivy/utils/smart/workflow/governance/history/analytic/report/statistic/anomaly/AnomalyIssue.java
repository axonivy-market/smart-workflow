package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

import java.util.List;
import java.util.Objects;

public record AnomalyIssue(AnomalyType type, List<Object> messageArguments) {

  public AnomalyIssue {
    Objects.requireNonNull(type, "type");
  }

  public String getMessage() {
    if (messageArguments == null || messageArguments.isEmpty()) {
      return type.getTemplate();
    }

    return type.getTemplate().formatted(messageArguments.toArray());
  }
}
