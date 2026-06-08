package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;


public enum ApprovalDecision {
  APPROVED         ("so-badge-green"),
  REJECTED         ("so-badge-red"),
  CHANGES_REQUESTED("so-badge-yellow");

  public final String badgeClass;

  ApprovalDecision(String badgeClass) {
    this.badgeClass = badgeClass;
  }

  public String getBadgeClass() { return badgeClass; }
}
