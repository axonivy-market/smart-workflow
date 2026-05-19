package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

public enum RiskKind {
  /** Finding produced by a deterministic missing-document presence check. */
  MISSING_DOC("/Enum/RiskKind/MISSING_DOC"),
  /** Finding produced by an AI validation step (policy, financial, cross-reference). */
  AI_VALIDATION("/Enum/RiskKind/AI_VALIDATION");

  private final String cmsUri;

  RiskKind(String cmsUri) {
    this.cmsUri = cmsUri;
  }

  public String getCmsUri() {
    return cmsUri;
  }
}
