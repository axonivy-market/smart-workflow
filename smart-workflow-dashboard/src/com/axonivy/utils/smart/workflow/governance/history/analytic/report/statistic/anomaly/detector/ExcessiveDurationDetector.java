package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class ExcessiveDurationDetector implements AnomalyDetector {

  private static final long THRESHOLD_MS = 30_000L;

  @Override
  public AnomalyType id() {
    return AnomalyType.EXCESSIVE_DURATION;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    if (context.durationMs() <= THRESHOLD_MS) {
      return List.of();
    }
    return List.of(issue(context.durationMs()));
  }
}
