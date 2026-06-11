package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ValidationFindingType;

public class ValidationFindingBuilder {

  private ValidationFindingBuilder() {
  }

  public static ValidationFinding of(FindingSeverity severity, String message, String source, RiskType riskType) {
    return createBase(severity, message, source, riskType);
  }

  public static ValidationFinding of(ValidationFindingType type, Object... args) {
    return createBase(type.getSeverity(), type.format(args), type.getSource(), type.getRiskType());
  }

  private static ValidationFinding createBase(FindingSeverity severity, String message,
      String source, RiskType riskType) {
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(severity);
    finding.setMessage(message);
    finding.setSource(source);
    finding.setRiskType(riskType);
    return finding;
  }
}
