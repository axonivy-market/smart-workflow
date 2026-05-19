package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

/**
 * A label-value pair stored inside an {@link AuditTrailEntry} for REQUEST-type entries.
 * Represents one field of the submitted {@link OnboardingRequest} at submission time.
 */
public class RequestSummaryLine {

  private String label;
  private String value;

  public RequestSummaryLine() {
  }

  public RequestSummaryLine(String label, String value) {
    this.label = label;
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
