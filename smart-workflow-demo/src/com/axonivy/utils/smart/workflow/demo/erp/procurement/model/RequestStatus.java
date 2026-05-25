package com.axonivy.utils.smart.workflow.demo.erp.procurement.model;

public enum RequestStatus {
  DRAFT("Draft"),
  SUBMITTED("Submitted");

  private final String displayName;

  RequestStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
