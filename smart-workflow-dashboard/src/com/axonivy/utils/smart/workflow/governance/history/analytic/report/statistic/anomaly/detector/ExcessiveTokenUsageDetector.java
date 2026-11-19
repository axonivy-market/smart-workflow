package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.List;
import java.util.Locale;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class ExcessiveTokenUsageDetector implements AnomalyDetector {

  private static final int THRESHOLD = 10_000;
  private static final String MESSAGE_FORMAT = "Tokens exceeded %s per conversation (%d)";

  @Override
  public AnomalyType id() {
    return AnomalyType.EXCESSIVE_TOKEN_USAGE;
  }

  @Override
  public String messageFormat() {
    return MESSAGE_FORMAT;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    if (context.totalTokens() <= THRESHOLD) {
      return List.of();
    }
    return List.of(issue(String.format(Locale.ROOT, "%,d", THRESHOLD), context.totalTokens()));
  }
}
