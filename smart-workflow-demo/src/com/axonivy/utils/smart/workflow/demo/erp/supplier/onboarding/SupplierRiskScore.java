package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

public class SupplierRiskScore {

  @Description("Financial stability score 0-100")
  private int financialStability;

  @Description("Policy compliance score 0-100")
  private int policyCompliance;

  @Description("Certificate validity score 0-100")
  private int certValidity;

  @Description("Aggregate risk score 0-100")
  private int aggregate;

  @Description("Risk level based on aggregate score")
  private RiskLevel level;

  public SupplierRiskScore() {
  }

  public SupplierRiskScore(int financialStability, int policyCompliance, int certValidity) {
    this.financialStability = financialStability;
    this.policyCompliance = policyCompliance;
    this.certValidity = certValidity;
    this.aggregate = calculateAggregate();
    this.level = RiskLevel.fromScore(this.aggregate);
  }

  private int calculateAggregate() {
    return (financialStability + policyCompliance + certValidity) / 3;
  }

  public static RiskLevel calculateLevel(int score) {
    return RiskLevel.fromScore(score);
  }

  public int getFinancialStability() {
    return financialStability;
  }

  public void setFinancialStability(int financialStability) {
    this.financialStability = financialStability;
  }

  public int getPolicyCompliance() {
    return policyCompliance;
  }

  public void setPolicyCompliance(int policyCompliance) {
    this.policyCompliance = policyCompliance;
  }

  public int getCertValidity() {
    return certValidity;
  }

  public void setCertValidity(int certValidity) {
    this.certValidity = certValidity;
  }

  public int getAggregate() {
    return aggregate;
  }

  public void setAggregate(int aggregate) {
    this.aggregate = aggregate;
  }

  public RiskLevel getLevel() {
    return level;
  }

  public void setLevel(RiskLevel level) {
    this.level = level;
  }
}
