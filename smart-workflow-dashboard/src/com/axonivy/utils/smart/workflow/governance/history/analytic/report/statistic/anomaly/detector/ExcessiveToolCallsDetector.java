package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class ExcessiveToolCallsDetector implements AnomalyDetector {

  private static final int THRESHOLD = 10;

  @Override
  public AnomalyType id() {
    return AnomalyType.EXCESSIVE_TOOL_CALLS;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    if (context.toolCallCount() <= THRESHOLD) {
      return List.of();
    }
    return List.of(issue(THRESHOLD, context.toolCallCount()));
  }
}
