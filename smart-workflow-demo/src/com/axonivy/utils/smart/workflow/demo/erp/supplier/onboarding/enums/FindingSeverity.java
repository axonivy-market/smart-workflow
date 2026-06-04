package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;


public enum FindingSeverity {
  PASSED ("so-finding-green",  "ti-circle-check",   "so-badge-green",  "so-log-line-ok",      0),
  WARNING("so-finding-yellow", "ti-alert-triangle",  "so-badge-yellow", "so-log-line-warning", 1),
  FAILURE("so-finding-red",    "ti-circle-x",        "so-badge-red",    "so-log-line-error",   2);

  public final String rowClass;
  public final String icon;
  public final String badgeClass;
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
