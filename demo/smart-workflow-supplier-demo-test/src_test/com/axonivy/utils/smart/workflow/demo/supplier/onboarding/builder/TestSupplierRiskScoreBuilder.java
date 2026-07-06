package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierRiskScoreBuilder {

  @Test
  void of_setsInputScoresAndComputesAggregateAsAverage() {
    SupplierRiskScore score = SupplierRiskScoreBuilder.of(70, 80, 90);
    assertThat(score.getFinancialStability()).isEqualTo(70);
    assertThat(score.getPolicyCompliance()).isEqualTo(80);
    assertThat(score.getCertValidity()).isEqualTo(90);

    assertThat(SupplierRiskScoreBuilder.of(60, 90, 60).getAggregate()).isEqualTo(70);
  }

  @Test
  void of_levels_greenYellowAndRed() {
    assertThat(SupplierRiskScoreBuilder.of(80, 90, 85).getLevel()).isEqualTo(RiskLevel.GREEN);
    assertThat(SupplierRiskScoreBuilder.of(45, 60, 70).getLevel()).isEqualTo(RiskLevel.YELLOW);
    assertThat(SupplierRiskScoreBuilder.of(10, 30, 20).getLevel()).isEqualTo(RiskLevel.RED);
  }

  @Test
  void of_exactBoundaries_greenAt80_yellowAt45() {
    SupplierRiskScore green = SupplierRiskScoreBuilder.of(80, 80, 80);
    assertThat(green.getAggregate()).isEqualTo(80);
    assertThat(green.getLevel()).isEqualTo(RiskLevel.GREEN);

    SupplierRiskScore yellow = SupplierRiskScoreBuilder.of(45, 45, 45);
    assertThat(yellow.getAggregate()).isEqualTo(45);
    assertThat(yellow.getLevel()).isEqualTo(RiskLevel.YELLOW);
  }
}
