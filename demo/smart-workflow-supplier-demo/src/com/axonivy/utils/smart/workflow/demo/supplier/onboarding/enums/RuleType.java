package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum RuleType {

  POLICY("compliance policy"),
  FINANCIAL("financial policy"),
  CERT_VALIDITY("cert validity");

  private final String label;

  RuleType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
