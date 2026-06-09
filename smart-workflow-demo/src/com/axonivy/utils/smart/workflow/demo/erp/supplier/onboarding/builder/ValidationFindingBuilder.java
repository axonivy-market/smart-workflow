package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ValidationFindingType;

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

  public static ValidationFinding of(ValidationFindingType type, Object... args) {
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(type.getSeverity());
    finding.setMessage(type.format(args));
    finding.setSource(type.getSource());
    finding.setRiskType(type.getRiskType());
    return finding;
  }
}
