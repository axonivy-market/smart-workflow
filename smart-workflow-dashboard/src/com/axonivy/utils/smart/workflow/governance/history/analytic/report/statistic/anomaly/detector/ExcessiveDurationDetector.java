package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.time.Duration;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class ExcessiveDurationDetector implements AnomalyDetector {

  private static final Duration THRESHOLD = Duration.ofSeconds(30);
  private static final String MESSAGE_FORMAT = "Duration exceeded %ds (%dms)";

  @Override
  public AnomalyType id() {
    return AnomalyType.EXCESSIVE_DURATION;
  }

  @Override
  public String messageFormat() {
    return MESSAGE_FORMAT;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    if (context.durationMs() <= THRESHOLD.toMillis()) {
      return List.of();
    }
    return List.of(issue(THRESHOLD.toSeconds(), context.durationMs()));
  }
}
