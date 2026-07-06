package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

public class SupplierRiskScoreBuilder {

  private SupplierRiskScoreBuilder() {
  }

  public static SupplierRiskScore of(int financialStability, int policyCompliance, int certValidity) {
    SupplierRiskScore score = new SupplierRiskScore();
    score.setFinancialStability(financialStability);
    score.setPolicyCompliance(policyCompliance);
    score.setCertValidity(certValidity);
    int aggregate = (financialStability + policyCompliance + certValidity) / 3;
    score.setAggregate(aggregate);
    score.setLevel(RiskLevel.fromScore(aggregate));
    return score;
  }
}
