package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum FindingSeverity {
  PASSED (0),
  WARNING(1),
  FAILURE(2);

  public final int rank;

  FindingSeverity(int rank) {
    this.rank = rank;
  }

  public int getRank() { return rank; }
}
