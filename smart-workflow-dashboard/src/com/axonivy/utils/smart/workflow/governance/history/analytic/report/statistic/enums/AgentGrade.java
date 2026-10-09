package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums;

public enum AgentGrade {
  A(90, "Excellent"),
  B(75, "Good"),
  C(60, "Fair, needs attention"),
  D(40, "Poor, investigate issues"),
  F(0,  "Critical, requires immediate action");

  private final int minScore;
  private final String feedback;

  AgentGrade(int minScore, String feedback) {
    this.minScore = minScore;
    this.feedback = feedback;
  }

  public int getMinScore() {
    return minScore;
  }

  public String getFeedback() {
    return feedback;
  }

  public String format(int score) {
    return "%s (%d/100) - %s".formatted(name(), score, feedback);
  }

  public static AgentGrade from(int score) {
    for (var grade : values()) {
      if (score >= grade.minScore) {
        return grade;
      }
    }
    return F;
  }
}
