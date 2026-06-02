package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

@Description("Severity level of a validation finding")
public enum FindingSeverity {
  PASSED ("so-finding-green",  "ti-circle-check",  "so-badge-green",  "so-log-line-ok",      0),
  WARNING("so-finding-yellow", "ti-alert-triangle", "so-badge-yellow", "so-log-line-warning", 1),
  FAILURE("so-finding-red",    "ti-circle-x",       "so-badge-red",    "so-log-line-error",   2);

  /** CSS class for finding card rows (so-finding-green / yellow / red). */
  public final String rowClass;
  /** Tabler icon name (ti-circle-check / ti-alert-triangle / ti-circle-x). */
  public final String icon;
  /** CSS class for severity badge (so-badge-green / yellow / red). */
  public final String badgeClass;
  /** CSS class for audit log line rows (so-log-line-ok / warning / error). */
  public final String logClass;
  /** Sort weight for ranking findings: FAILURE=2, WARNING=1, PASSED=0. */
  public final int rank;

  FindingSeverity(String rowClass, String icon, String badgeClass, String logClass, int rank) {
    this.rowClass   = rowClass;
    this.icon       = icon;
    this.badgeClass = badgeClass;
    this.logClass   = logClass;
    this.rank       = rank;
  }
}
