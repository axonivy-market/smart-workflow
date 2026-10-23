package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;

public class TruncatedOutputDetector implements AnomalyDetector {

  @Override
  public AnomalyType id() {
    return AnomalyType.TRUNCATED_OUTPUT;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    if (!FinishReasons.LENGTH.matches(context.finishReason())) {
      return List.of();
    }
    return List.of(issue());
  }
}
