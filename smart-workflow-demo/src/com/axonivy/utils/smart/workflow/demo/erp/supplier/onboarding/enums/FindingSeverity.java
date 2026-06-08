package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

public enum FindingSeverity {
  PASSED (0),
  WARNING(1),
  FAILURE(2);

  /** Sort weight for ranking findings: FAILURE=2, WARNING=1, PASSED=0. */
  public final int rank;

  FindingSeverity(int rank) {
    this.rank = rank;
  }

  public int getRank() { return rank; }
}
