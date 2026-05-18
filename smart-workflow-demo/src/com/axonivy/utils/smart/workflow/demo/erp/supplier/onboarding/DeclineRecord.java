package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.util.List;

public class DeclineRecord {

  private String declinedAt;
  private String declinedBy;
  private List<String> declineReasons;
  private int riskScoreAggregate;
  private String riskScoreLevel;

  public String getDeclinedAt() {
    return declinedAt;
  }

  public void setDeclinedAt(String declinedAt) {
    this.declinedAt = declinedAt;
  }

  public String getDeclinedBy() {
    return declinedBy;
  }

  public void setDeclinedBy(String declinedBy) {
    this.declinedBy = declinedBy;
  }

  public List<String> getDeclineReasons() {
    return declineReasons;
  }

  public void setDeclineReasons(List<String> declineReasons) {
    this.declineReasons = declineReasons;
  }

  public int getRiskScoreAggregate() {
    return riskScoreAggregate;
  }

  public void setRiskScoreAggregate(int riskScoreAggregate) {
    this.riskScoreAggregate = riskScoreAggregate;
  }

  public String getRiskScoreLevel() {
    return riskScoreLevel;
  }

  public void setRiskScoreLevel(String riskScoreLevel) {
    this.riskScoreLevel = riskScoreLevel;
  }
}
