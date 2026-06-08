package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.RiskLevel;

public class SupplierRiskScoreBuilder {

  private SupplierRiskScoreBuilder() {
  }

  /**
   * Creates a {@link SupplierRiskScore} from the three component scores,
   * calculates the aggregate and derives the risk level.
   *
   * @param financialStability  financial stability score 0-100
   * @param policyCompliance    policy compliance score 0-100
   * @param certValidity        certificate validity score 0-100
   * @return a fully populated SupplierRiskScore
   */
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
