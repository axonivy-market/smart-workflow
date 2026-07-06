package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestRiskLevelSupport {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "getRiskLevel_whenGreen_returnsGreen,   GREEN,  GREEN",
      "getRiskLevel_whenYellow_returnsYellow, YELLOW, YELLOW",
      "getRiskLevel_whenRed_returnsRed,       RED,    RED"
  })
  void getRiskLevel(String testName, RiskLevel level, RiskLevel expected) {
    assertThat(support(level).getRiskLevel()).as(testName).isEqualTo(expected);
  }

  @Test
  void getRiskLevel_whenNoValidScore_defaultsToYellow() {
    assertThat(((RiskLevelSupport) () -> null).getRiskLevel()).isEqualTo(RiskLevel.YELLOW);
    assertThat(((RiskLevelSupport) () -> new SupplierAgentResponse()).getRiskLevel()).isEqualTo(RiskLevel.YELLOW);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "cssClasses_whenGreen,  GREEN,  so-agent-decision-green,  so-score-circle-green,  so-success-banner, so-badge-green",
      "cssClasses_whenYellow, YELLOW, so-agent-decision-yellow, so-score-circle-yellow, '',                so-badge-yellow",
      "cssClasses_whenRed,    RED,    so-agent-decision-red,    so-score-circle-red,    so-decline-banner, so-badge-red"
  })
  void cssClasses_reflectRiskLevel(String testName, RiskLevel level,
      String decisionBox, String scoreCircle, String bannerModifier, String badgeClass) {
    RiskLevelSupport s = support(level);
    assertThat(s.getDecisionBoxCssClass()).as(testName + ".decisionBox").isEqualTo(decisionBox);
    assertThat(s.getScoreCircleCssClass()).as(testName + ".scoreCircle").isEqualTo(scoreCircle);
    assertThat(s.getBannerModifierClass()).as(testName + ".bannerModifier").isEqualTo(bannerModifier);
    assertThat(s.getBannerBadgeClass()).as(testName + ".badgeClass").isEqualTo(badgeClass);
  }

  private static RiskLevelSupport support(RiskLevel level) {
    SupplierRiskScore score = new SupplierRiskScore();
    score.setLevel(level);
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setRiskScore(score);
    return () -> response;
  }
}
