package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;

import dev.langchain4j.model.output.structured.Description;

public class SupplierPolicyRule {

  @Description("Unique target key for this rule (used to map findings), e.g. RULE_01_COMMERCIAL_REGISTER")
  private String target;

  @Description("Human-readable rule text")
  private String rule;

  @Description("Risk score deduction used when this rule is violated")
  private int riskScore;

  @Description("Runtime evaluation flag: true if passed for the current supplier, false otherwise")
  private boolean passed;

  @Description("The type of rule this is, e.g. policy rule or financial rule")
  private RuleType ruleType;

  /** The LegalDocumentType this rule evaluates, or null if not document-type-specific. */
  private LegalDocumentType legalDocumentType;

  /** The certification sub-type (LegalDocumentType) this rule evaluates, or null if not certification-specific. */
  private LegalDocumentType certificationType;

  public SupplierPolicyRule() {
  }
  
  public SupplierPolicyRule(String target, String rule, int riskScore, boolean passed, RuleType ruleType) {
    this.target = target;
    this.rule = rule;
    this.riskScore = riskScore;
    this.passed = passed;
    this.ruleType = ruleType;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(int riskScore) {
    this.riskScore = riskScore;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public LegalDocumentType getLegalDocumentType() {
    return legalDocumentType;
  }

  public void setLegalDocumentType(LegalDocumentType legalDocumentType) {
    this.legalDocumentType = legalDocumentType;
  }

  public LegalDocumentType getCertificationType() {
    return certificationType;
  }

  public void setCertificationType(LegalDocumentType certificationType) {
    this.certificationType = certificationType;
  }

  public RuleType getRuleType() {
    return ruleType;
  }

  public void setRuleType(RuleType ruleType) {
    this.ruleType = ruleType;
  }
}
