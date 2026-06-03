package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

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
