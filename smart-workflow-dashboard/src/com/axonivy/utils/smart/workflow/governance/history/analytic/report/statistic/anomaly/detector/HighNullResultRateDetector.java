package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class HighNullResultRateDetector implements AnomalyDetector {

  private static final double THRESHOLD = 0.5;

  @Override
  public AnomalyType id() {
    return AnomalyType.HIGH_NULL_RESULT_RATE;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    return context.toolSummaries().stream()
        .filter(tool -> tool.getCallCount() > 0
            && (double) tool.getNullResultCount() / tool.getCallCount() > THRESHOLD)
        .map(tool -> issue(tool.getToolName(), tool.getNullResultCount(), tool.getCallCount()))
        .toList();
  }
}
