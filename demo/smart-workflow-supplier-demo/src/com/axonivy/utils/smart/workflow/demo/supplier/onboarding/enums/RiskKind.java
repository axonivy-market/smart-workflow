package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum RiskKind {
  MISSING_DOC("/Enum/RiskKind/MISSING_DOC"),
  AI_VALIDATION("/Enum/RiskKind/AI_VALIDATION");

  private final String cmsUri;

  RiskKind(String cmsUri) {
    this.cmsUri = cmsUri;
  }

  public String getCmsUri() {
    return cmsUri;
  }
}
