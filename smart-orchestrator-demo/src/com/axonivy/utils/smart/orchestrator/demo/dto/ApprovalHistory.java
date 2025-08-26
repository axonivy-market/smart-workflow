package com.axonivy.utils.smart.orchestrator.demo.dto;

public class ApprovalHistory {
  private String approver;
  private Boolean isApprove;
  private String comment;

  public String getApprover() {
    return approver;
  }

  public void setApprover(String approver) {
    this.approver = approver;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Boolean getIsApprove() {
    return isApprove;
  }

  public void setIsApprove(Boolean isApprove) {
    this.isApprove = isApprove;
  }
}