package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

import java.util.Arrays;
import java.util.List;

public interface AnomalyDetector {

  AnomalyType id();

  String messageFormat();

  List<AnomalyIssue> detect(AnomalyDetectionContext context);

  default AnomalyIssue issue(Object... messageArguments) {
    return new AnomalyIssue(id(), Arrays.asList(messageArguments));
  }
}
