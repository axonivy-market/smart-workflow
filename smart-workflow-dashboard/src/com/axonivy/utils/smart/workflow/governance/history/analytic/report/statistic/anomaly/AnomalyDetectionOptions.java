package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record AnomalyDetectionOptions(Set<AnomalyType> enabledTypes) {

  public AnomalyDetectionOptions {
    enabledTypes = Set.copyOf(Objects.requireNonNull(enabledTypes, "enabledTypes"));
  }

  public static AnomalyDetectionOptions defaults() {
    return new AnomalyDetectionOptions(EnumSet.allOf(AnomalyType.class));
  }

  public boolean isEnabled(AnomalyType type) {
    return enabledTypes.contains(type);
  }
}
