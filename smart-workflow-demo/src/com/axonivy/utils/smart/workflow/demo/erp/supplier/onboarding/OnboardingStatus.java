package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

@Description("Current status of the supplier onboarding workflow")
public enum OnboardingStatus {

  REQUEST("Initial Request"),
  DB_CHECK("Duplicate Check"),
  SUPPLIER_DATA("Supplier Data Entry"),
  VALIDATION("Agent Validation"),
  RISK_SCORING("Risk Scoring"),
  APPROVAL_PENDING("Awaiting Approval"),
  CLARIFICATION_REQUIRED("Clarification Required"),
  DECLINED("Declined"),
  COMPLETED("Completed");

  private final String label;

  OnboardingStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
