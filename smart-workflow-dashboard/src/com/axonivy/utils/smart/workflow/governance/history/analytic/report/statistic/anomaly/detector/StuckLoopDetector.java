package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.detector;

import java.util.stream.Collectors;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetector;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;

public class StuckLoopDetector implements AnomalyDetector {

  private static final int THRESHOLD = 5;
  private static final String MESSAGE_FORMAT = "Tool '%s' called %d times with identical arguments: possible stuck loop";

  private record ToolCallKey(String toolName, String arguments) {}

  @Override
  public AnomalyType id() {
    return AnomalyType.STUCK_LOOP;
  }

  @Override
  public String messageFormat() {
    return MESSAGE_FORMAT;
  }

  @Override
  public List<AnomalyIssue> detect(AnomalyDetectionContext context) {
    return context.toolExecutions().stream()
        .filter(tool -> tool.arguments() != null)
        .collect(Collectors.groupingBy(
            tool -> new ToolCallKey(tool.toolName(), tool.arguments()),
            Collectors.counting()))
        .entrySet().stream()
        .filter(entry -> entry.getValue() > THRESHOLD)
        .map(entry -> issue(entry.getKey().toolName(), entry.getValue()))
        .toList();
  }
}
