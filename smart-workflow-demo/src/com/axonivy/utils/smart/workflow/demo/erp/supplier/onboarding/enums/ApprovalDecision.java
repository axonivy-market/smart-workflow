package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

import dev.langchain4j.model.output.structured.Description;

@Description("Decision captured in an approval stage")
public enum ApprovalDecision {
  APPROVED         ("so-badge-green"),
  REJECTED         ("so-badge-red"),
  CHANGES_REQUESTED("so-badge-yellow");

  public final String badgeClass;

  ApprovalDecision(String badgeClass) {
    this.badgeClass = badgeClass;
  }
}
