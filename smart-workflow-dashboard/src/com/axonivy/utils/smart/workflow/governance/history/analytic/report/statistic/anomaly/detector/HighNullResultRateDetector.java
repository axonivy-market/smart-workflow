package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class HighNullResultRateDetector implements AnomalyDetector {

  private static final int THRESHOLD_PERCENT = 50;
  private static final double THRESHOLD = THRESHOLD_PERCENT / 100.0;
  private static final String MESSAGE_FORMAT = "Tool '%s' returned null/empty in %d/%d calls (>%d%%)";

  @Override
  public AnomalyType id() {
    return AnomalyType.HIGH_NULL_RESULT_RATE;
  }

  @Override
  public String messageFormat() {
    return MESSAGE_FORMAT;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    return context.toolSummaries().stream()
        .filter(tool -> tool.getCallCount() > 0
            && (double) tool.getNullResultCount() / tool.getCallCount() > THRESHOLD)
        .map(tool -> issue(tool.getToolName(), tool.getNullResultCount(), tool.getCallCount(), THRESHOLD_PERCENT))
        .toList();
  }
}
