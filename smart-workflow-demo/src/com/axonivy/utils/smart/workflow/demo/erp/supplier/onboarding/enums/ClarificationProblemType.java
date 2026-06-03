package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

public enum ClarificationProblemType {
  /** Missing or expired document / certification — show SingleLegalDocument upload. */
  DOCUMENT,
  /** Potential ERP duplicate found — show explanation textarea. */
  DUPLICATE,
  /** General / policy finding — show explanation textarea. */
  OTHER
}
