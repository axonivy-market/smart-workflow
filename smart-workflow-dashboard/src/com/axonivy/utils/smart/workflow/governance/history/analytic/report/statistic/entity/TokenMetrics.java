package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

public record TokenMetrics(
    TokenUsageSummary usage,
    double avgTokensPerMsg,
    double outputInputRatio,
    double estimatedInputCost,
    double estimatedOutputCost) {

  public TokenUsageSummary getUsage() {
    return usage;
  }
  public int getTotalTokens() {
    return usage == null ? 0 : usage.getTotalTokens();
  }
  public double getAvgTokensPerMsg() {
    return avgTokensPerMsg;
  }
  public double getOutputInputRatio() {
    return outputInputRatio;
  }
  public double getEstimatedInputCost() {
    return estimatedInputCost;
  }
  public double getEstimatedOutputCost() {
    return estimatedOutputCost;
  }
}
