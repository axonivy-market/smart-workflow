package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

/**
 * Records how a single clarification item was resolved by the supplier.
 * Stored inside an {@link AuditTrailEntry} when a clarification cycle is submitted.
 */
public class ResolvedClarificationItem {

  /** The original problem description from the clarification item. */
  private String problem;

  /** How the item was addressed: "Document uploaded" or "Explanation provided". */
  private String resolutionType;

  /** Optional explanation text entered by the user. Null for document-upload resolutions. */
  private String userExplanation;

  public ResolvedClarificationItem() {
  }

  public ResolvedClarificationItem(String problem, String resolutionType, String userExplanation) {
    this.problem = problem;
    this.resolutionType = resolutionType;
    this.userExplanation = userExplanation;
  }

  public String getProblem() {
    return problem;
  }

  public void setProblem(String problem) {
    this.problem = problem;
  }

  public String getResolutionType() {
    return resolutionType;
  }

  public void setResolutionType(String resolutionType) {
    this.resolutionType = resolutionType;
  }

  public String getUserExplanation() {
    return userExplanation;
  }

  public void setUserExplanation(String userExplanation) {
    this.userExplanation = userExplanation;
  }
}
