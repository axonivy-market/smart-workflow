package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;

public enum ValidationFindingType {

  COMPANY_REGISTER_MISSING("Company Register", FindingSeverity.WARNING, RiskType.POLICY_COMPLIANCE,
      "No commercial register number provided — manual verification required"),
  COMPANY_REGISTER_VALID_DE("Company Register", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "Company Register: %s — active, no insolvency proceedings"),
  COMPANY_REGISTER_FORMAT_INVALID_DE("Company Register", FindingSeverity.WARNING, RiskType.POLICY_COMPLIANCE,
      "Company Register: %s — format invalid (expected HRB/HRA + digits for DE)"),
  COMPANY_REGISTER_VALID("Company Register", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "Company Register: %s — company registration document verified"),

  VAT_ID_MISSING("VAT Validation", FindingSeverity.WARNING, RiskType.POLICY_COMPLIANCE,
      "No VAT ID provided — may be exempt for sole traders or certain legal forms"),
  VAT_ID_NO_RULE("VAT Validation", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "VAT ID %s — accepted (no country-specific format rule configured)"),
  VAT_ID_CONFIRMED("VAT Validation", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "VAT ID %s — confirmed"),
  VAT_ID_INVALID("VAT Validation", FindingSeverity.FAILURE, RiskType.POLICY_COMPLIANCE,
      "VAT ID %s — format invalid for country %s (expected pattern: %s)"),

  ERP_NO_SIMILAR("ERP Duplicate Check", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "No duplicate in ERP — no similar suppliers found"),
  ERP_NO_DUPLICATE("ERP Duplicate Check", FindingSeverity.PASSED, RiskType.POLICY_COMPLIANCE,
      "No duplicate in ERP (distinct from existing supplier record)"),
  ERP_DUPLICATE_FOUND("ERP Duplicate Check", FindingSeverity.WARNING, RiskType.POLICY_COMPLIANCE,
      "Possible ERP duplicate: %d similar supplier(s) found — manual review recommended");

  private final String source;
  private final FindingSeverity severity;
  private final RiskType riskType;
  private final String template;

  ValidationFindingType(String source, FindingSeverity severity, RiskType riskType, String template) {
    this.source = source;
    this.severity = severity;
    this.riskType = riskType;
    this.template = template;
  }

  public String getSource() {
    return source;
  }

  public FindingSeverity getSeverity() {
    return severity;
  }

  public RiskType getRiskType() {
    return riskType;
  }

  public String format(Object... args) {
    return args.length == 0 ? template : String.format(template, args);
  }
}
