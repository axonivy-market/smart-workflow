package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

public class ApprovalRecord {

  @Description("Approval stage, for example SUPERVISOR or QM_ISM")
  private ApprovalStage stage;

  @Description("Display name of the approver")
  private String actorDisplayName;

  @Description("Decision taken by the approver")
  private ApprovalDecision decision;

  @Description("Optional approval comment")
  private String comment;

  @Description("Decision timestamp in ISO-8601 text format")
  private String decidedAt;

  public ApprovalStage getStage() {
    return stage;
  }

  public void setStage(ApprovalStage stage) {
    this.stage = stage;
  }

  public String getActorDisplayName() {
    return actorDisplayName;
  }

  public void setActorDisplayName(String actorDisplayName) {
    this.actorDisplayName = actorDisplayName;
  }

  public ApprovalDecision getDecision() {
    return decision;
  }

  public void setDecision(ApprovalDecision decision) {
    this.decision = decision;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(String decidedAt) {
    this.decidedAt = decidedAt;
  }
}
