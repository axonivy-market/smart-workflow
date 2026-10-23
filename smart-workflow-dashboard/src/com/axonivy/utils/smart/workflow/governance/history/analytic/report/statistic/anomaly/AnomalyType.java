package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

public enum AnomalyType {
  EXCESSIVE_DURATION,
  EXCESSIVE_TOKEN_USAGE,
  EXCESSIVE_TOOL_CALLS,
  FATAL_GUARDRAIL_RESULTS,
  TRUNCATED_OUTPUT,
  HIGH_NULL_RESULT_RATE,
  STUCK_LOOP
}
