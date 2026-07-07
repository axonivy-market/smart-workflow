package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

import ch.ivyteam.ivy.environment.Ivy;

public interface RiskLevelSupport extends AgentResultView {

  default RiskLevel getRiskLevel() {
    var response = getAgentResponse();
    return Optional.ofNullable(response)
        .map(r -> r.getRiskScore())
        .map(s -> s.getLevel())
        .orElse(RiskLevel.YELLOW);
  }

  default String getDecisionBoxCssClass() {
    RiskLevel level = getRiskLevel();
    return switch (level) {
      case GREEN -> "so-agent-decision-green";
      case RED   -> "so-agent-decision-red";
      default    -> "so-agent-decision-yellow";
    };
  }

  default String getScoreCircleCssClass() {
    RiskLevel level = getRiskLevel();
    return switch (level) {
      case GREEN -> "so-score-circle-green";
      case RED   -> "so-score-circle-red";
      default    -> "so-score-circle-yellow";
    };
  }

  default String getBannerModifierClass() {
    return switch (getRiskLevel()) {
      case GREEN -> "so-success-banner";
      case RED   -> "so-decline-banner";
      default    -> "";
    };
  }

  default String getBannerBadgeClass() {
    return switch (getRiskLevel()) {
      case GREEN -> "so-badge-green";
      case RED   -> "so-badge-red";
      default    -> "so-badge-yellow";
    };
  }

  default String getThresholdLabel() {
    RiskLevel level = getRiskLevel();
    return switch (level) {
      case GREEN  -> Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "ThresholdGreen");
      case YELLOW -> Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "ThresholdYellow");
      default     -> Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "ThresholdRed");
    };
  }
}
