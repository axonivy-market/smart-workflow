package com.axonivy.utils.smart.workflow.demo.dto;

import dev.langchain4j.model.output.structured.Description;

public class AiApprovalDecision {
  public static enum AiApprovalDecisionEnum {
    APPROVE, REJECT, WARNING;
  }

  @Description("Your decision. This is an enum field")
  private AiApprovalDecisionEnum decision;
  @Description("The reason why you choose the decision. This field should be plain text.")
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
