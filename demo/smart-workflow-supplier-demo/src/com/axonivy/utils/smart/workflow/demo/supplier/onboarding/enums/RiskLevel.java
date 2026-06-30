package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum RiskLevel {

  GREEN(80, "Low Risk — Approval route"),
  YELLOW(45, "Medium Risk — Clarification needed"),
  RED(0, "High Risk — Automatic decline");

  private final int minScore;
  private final String description;

  RiskLevel(int minScore, String description) {
    this.minScore = minScore;
    this.description = description;
  }

  public int getMinScore() {
    return minScore;
  }

  public String getDescription() {
    return description;
  }

  public static RiskLevel fromScore(int score) {
    if (score >= GREEN.minScore) {
      return GREEN;
    } else if (score >= YELLOW.minScore) {
      return YELLOW;
    }
    return RED;
  }
}
