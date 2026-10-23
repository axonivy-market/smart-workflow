package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class FatalGuardrailResultsDetector implements AnomalyDetector {

  @Override
  public AnomalyType id() {
    return AnomalyType.FATAL_GUARDRAIL_RESULTS;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    return context.guardrailSummaries().stream()
        .filter(guardrail -> guardrail.getFatalCount() > 0)
        .map(guardrail -> issue(guardrail.getGuardrailName(), guardrail.getFatalCount()))
        .toList();
  }
}
