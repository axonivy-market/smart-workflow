package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum LogLineSeverity {
  OK("Ok"),
  WARNING("Warning"),
  ERROR("Error");

  private final String description;

  LogLineSeverity(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
