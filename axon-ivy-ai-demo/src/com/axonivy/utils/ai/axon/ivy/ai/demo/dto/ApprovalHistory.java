package com.axonivy.utils.ai.axon.ivy.ai.demo.dto;

public class ApprovalHistory {
  private String approver;
  private boolean isApprove;
  private String comment;

  public String getApprover() {
    return approver;
  }

  public void setApprover(String approver) {
    this.approver = approver;
  }

  public boolean isApprove() {
    return isApprove;
  }

  public void setApprove(boolean isApprove) {
    this.isApprove = isApprove;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}