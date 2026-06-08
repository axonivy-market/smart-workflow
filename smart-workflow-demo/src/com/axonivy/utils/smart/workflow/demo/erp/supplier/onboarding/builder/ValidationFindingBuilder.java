package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;

public class ValidationFindingBuilder {

  private ValidationFindingBuilder() {
  }

  public static ValidationFinding of(FindingSeverity severity, String message, String source, RiskType riskType) {
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(severity);
    finding.setMessage(message);
    finding.setSource(source);
    finding.setRiskType(riskType);
    return finding;
  }
}
