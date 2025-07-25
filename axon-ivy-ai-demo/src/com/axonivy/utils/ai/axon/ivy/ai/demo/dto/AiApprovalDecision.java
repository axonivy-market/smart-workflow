package com.axonivy.utils.ai.axon.ivy.ai.demo.dto;

public class AiApprovalDecision {
  public static enum AiApprovalDecisionEnum {
    APPROVE, REJECT, WARNING;
  }

  private AiApprovalDecisionEnum decision;
  private String reason;

  public AiApprovalDecisionEnum getDecision() {
    return decision;
  }

  public void setDecision(AiApprovalDecisionEnum decision) {
    this.decision = decision;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
