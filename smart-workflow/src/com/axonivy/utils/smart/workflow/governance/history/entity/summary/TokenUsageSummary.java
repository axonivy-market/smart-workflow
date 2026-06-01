package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

public class TokenUsageSummary {

  private int totalInputTokens;
  private int totalOutputTokens;
  private int totalTokens;
  private double avgInputTokens;
  private double avgOutputTokens;
  private double avgTotalTokens;
  private int maxSingleConversationTokens;

  public TokenUsageSummary() {}

  public TokenUsageSummary(int totalInputTokens, int totalOutputTokens, int totalTokens,
      double avgInputTokens, double avgOutputTokens, double avgTotalTokens,
      int maxSingleConversationTokens) {
    this.totalInputTokens = totalInputTokens;
    this.totalOutputTokens = totalOutputTokens;
    this.totalTokens = totalTokens;
    this.avgInputTokens = avgInputTokens;
    this.avgOutputTokens = avgOutputTokens;
    this.avgTotalTokens = avgTotalTokens;
    this.maxSingleConversationTokens = maxSingleConversationTokens;
  }

  public int getTotalInputTokens() { return totalInputTokens; }
  public void setTotalInputTokens(int totalInputTokens) { this.totalInputTokens = totalInputTokens; }

  public int getTotalOutputTokens() { return totalOutputTokens; }
  public void setTotalOutputTokens(int totalOutputTokens) { this.totalOutputTokens = totalOutputTokens; }

  public int getTotalTokens() { return totalTokens; }
  public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

  public double getAvgInputTokens() { return avgInputTokens; }
  public void setAvgInputTokens(double avgInputTokens) { this.avgInputTokens = avgInputTokens; }

  public double getAvgOutputTokens() { return avgOutputTokens; }
  public void setAvgOutputTokens(double avgOutputTokens) { this.avgOutputTokens = avgOutputTokens; }

  public double getAvgTotalTokens() { return avgTotalTokens; }
  public void setAvgTotalTokens(double avgTotalTokens) { this.avgTotalTokens = avgTotalTokens; }

  public int getMaxSingleConversationTokens() { return maxSingleConversationTokens; }
  public void setMaxSingleConversationTokens(int maxSingleConversationTokens) { this.maxSingleConversationTokens = maxSingleConversationTokens; }
}
