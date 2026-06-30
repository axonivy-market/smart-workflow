package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum AgentStepStatus {
  PENDING("Pending"),
  RUNNING("Running"),
  COMPLETED("Completed"),
  FAILED("Failed");

  private final String description;

  AgentStepStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
