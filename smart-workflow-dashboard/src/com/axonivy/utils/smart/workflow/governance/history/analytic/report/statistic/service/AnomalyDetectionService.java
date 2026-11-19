package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionOptions;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.ExcessiveDurationDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.ExcessiveTokenUsageDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.ExcessiveToolCallsDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.FatalGuardrailResultsDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.HighNullResultRateDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.StuckLoopDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector.TruncatedOutputDetector;

public class AnomalyDetectionService {

  private final List<AnomalyDetector> detectors;

  public AnomalyDetectionService() {
    this(List.of(
        new ExcessiveDurationDetector(),
        new ExcessiveTokenUsageDetector(),
        new ExcessiveToolCallsDetector(),
        new FatalGuardrailResultsDetector(),
        new TruncatedOutputDetector(),
        new HighNullResultRateDetector(),
        new StuckLoopDetector()));
  }

  public AnomalyDetectionService(List<AnomalyDetector> detectors) {
    this.detectors = List.copyOf(Objects.requireNonNull(detectors, "detectors"));
    Set<AnomalyType> ids = new HashSet<>();
    for (AnomalyDetector detector : this.detectors) {
      Objects.requireNonNull(detector, "detector");
      if (!ids.add(detector.id())) {
        throw new IllegalArgumentException("Duplicate anomaly detector id: " + detector.id());
      }
    }
  }

  public List<AnomalyIssue> detect(AnomalyDetectionContext context, AnomalyDetectionOptions options) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(options, "options");
    return detectors.stream()
        .filter(detector -> options.isEnabled(detector.id()))
        .flatMap(detector -> detector.detect(context).stream())
        .toList();
  }
}
