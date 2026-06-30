package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestRiskScoreHelper {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "boost_whenAllResolved_50,         50,  1.0,  100",
      "boost_whenAllResolved_0,          0,   1.0,  100",
      "boost_whenNoneResolved_50,        50,  0.0,  50",
      "boost_whenNoneResolved_30,        30,  0.0,  30",
      "boost_whenPartiallyResolved_60,   60,  0.5,  80",
      "boost_whenPartiallyResolved_40,   40,  0.25, 55",
      "boost_whenOriginalIs100_ratio0,   100, 0.0,  100",
      "boost_whenOriginalIs100_ratio0_5, 100, 0.5,  100",
      "boost_whenOriginalIs100_ratio1,   100, 1.0,  100"
  })
  void boost(String testName, int original, double ratio, int expected) {
    assertThat(RiskScoreHelper.boost(original, ratio)).as(testName).isEqualTo(expected);
  }

  @Test
  void recalculateScore_whenAllFindingsResolved_boostsAllComponents() {
    SupplierRiskScore score = score(50, 50, 50);
    List<ValidationFinding> findings = List.of(resolved(), resolved());

    RiskScoreHelper.recalculateScore(score, findings, 50, 50, 50);

    assertThat(score.getFinancialStability()).isEqualTo(100);
    assertThat(score.getPolicyCompliance()).isEqualTo(100);
    assertThat(score.getCertValidity()).isEqualTo(100);
    assertThat(score.getAggregate()).isEqualTo(100);
    assertThat(score.getLevel()).isEqualTo(RiskLevel.GREEN);
  }

  @Test
  void recalculateScore_whenNoFindingsResolved_keepsOriginalScores() {
    SupplierRiskScore score = score(50, 50, 50);
    List<ValidationFinding> findings = List.of(unresolved(), unresolved());

    RiskScoreHelper.recalculateScore(score, findings, 50, 50, 50);

    assertThat(score.getFinancialStability()).isEqualTo(50);
    assertThat(score.getPolicyCompliance()).isEqualTo(50);
    assertThat(score.getCertValidity()).isEqualTo(50);
    assertThat(score.getAggregate()).isEqualTo(50);
  }

  @Test
  void recalculateScore_whenFindingsNullOrEmpty_doesNothing() {
    SupplierRiskScore score = score(50, 50, 50);
    RiskScoreHelper.recalculateScore(score, List.of(), 50, 50, 50);
    assertThat(score.getFinancialStability()).isEqualTo(50);

    score = score(50, 50, 50);
    RiskScoreHelper.recalculateScore(score, null, 50, 50, 50);
    assertThat(score.getFinancialStability()).isEqualTo(50);
  }

  @Test
  void recalculateScore_whenScoreNull_doesNothing() {
    RiskScoreHelper.recalculateScore(null, List.of(resolved()), 50, 50, 50);
  }

  @Test
  void recalculateScore_setsCorrectRiskLevel() {
    SupplierRiskScore score = score(30, 30, 30);
    List<ValidationFinding> findings = List.of(resolved(), resolved());

    RiskScoreHelper.recalculateScore(score, findings, 30, 30, 30);

    assertThat(score.getLevel()).isEqualTo(RiskLevel.GREEN);
  }

  @Test
  void recalculateScore_setsYellowLevel_whenAggregateInMiddleRange() {
    SupplierRiskScore score = score(50, 50, 50);
    List<ValidationFinding> findings = List.of(unresolved());

    RiskScoreHelper.recalculateScore(score, findings, 50, 50, 50);

    assertThat(score.getLevel()).isEqualTo(RiskLevel.YELLOW);
  }

  private static SupplierRiskScore score(int financial, int policy, int cert) {
    SupplierRiskScore s = new SupplierRiskScore();
    s.setFinancialStability(financial);
    s.setPolicyCompliance(policy);
    s.setCertValidity(cert);
    return s;
  }

  private static ValidationFinding resolved() {
    ValidationFinding f = new ValidationFinding();
    f.setResolved(Boolean.TRUE);
    return f;
  }

  private static ValidationFinding unresolved() {
    ValidationFinding f = new ValidationFinding();
    f.setResolved(Boolean.FALSE);
    return f;
  }
}
