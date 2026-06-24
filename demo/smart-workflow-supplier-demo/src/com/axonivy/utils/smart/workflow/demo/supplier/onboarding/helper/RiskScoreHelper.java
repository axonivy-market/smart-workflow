package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

public final class RiskScoreHelper {

  private RiskScoreHelper() {}

  public static void recalculateScore(SupplierRiskScore score,
      List<ValidationFinding> findings,
      int originalFinancial, int originalPolicy, int originalCert) {
    if (score == null || findings == null || findings.isEmpty()) {
      return;
    }
    long resolved = findings.stream()
        .filter(f -> Boolean.TRUE.equals(f.getResolved())).count();
    double ratio = (double) resolved / findings.size();

    int newFinancial = boost(originalFinancial, ratio);
    int newPolicy    = boost(originalPolicy,    ratio);
    int newCert      = boost(originalCert,      ratio);
    int newAggregate = (newFinancial + newPolicy + newCert) / 3;

    score.setFinancialStability(newFinancial);
    score.setPolicyCompliance(newPolicy);
    score.setCertValidity(newCert);
    score.setAggregate(newAggregate);
    score.setLevel(RiskLevel.fromScore(newAggregate));
  }

  static int boost(int original, double resolvedRatio) {
    return Math.min(100, (int) Math.round(original + (100 - original) * resolvedRatio));
  }
}
