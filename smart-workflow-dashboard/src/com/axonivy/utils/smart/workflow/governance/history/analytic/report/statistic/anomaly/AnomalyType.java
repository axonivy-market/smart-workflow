package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

public enum AnomalyType {
  EXCESSIVE_DURATION("Duration exceeded 30s (%dms)"),
  EXCESSIVE_TOKEN_USAGE("Tokens exceeded 10,000 per conversation (%d)"),
  EXCESSIVE_TOOL_CALLS("More than %d tool calls (%d): possible loop"),
  FATAL_GUARDRAIL_RESULTS("FATAL guardrail result on '%s' (%d time(s))"),
  TRUNCATED_OUTPUT("Finish reason is LENGTH: output was truncated"),
  HIGH_NULL_RESULT_RATE("Tool '%s' returned null/empty in %d/%d calls (>50%%)"),
  STUCK_LOOP("Tool '%s' called %d times with identical arguments: possible stuck loop");

  private final String messageTemplate;

  AnomalyType(String messageTemplate) {
    this.messageTemplate = messageTemplate;
  }

  public String getTemplate() {
    return messageTemplate;
  }
}
