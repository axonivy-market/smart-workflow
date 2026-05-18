package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

/**
 * A single actionable clarification item shown to the supplier requester
 * on the Clarification task (Screen 07).
 */
public class ClarificationItem {

  /** Human-readable problem description (from ValidationFinding.message). */
  private String message;

  /** Type of problem — drives which resolve UI to show. */
  private ClarificationProblemType problemType;

  /**
   * For DOCUMENT type: the document type key to pre-target the upload.
   * Examples: "CERTIFICATION:ISO_9001", "COMMERCIAL_REGISTER", "SELF_DECLARATION", "ANNUAL_REPORT".
   * Null for DUPLICATE / OTHER types.
   */
  private String documentTypeKey;

  /**
   * Optional explanation text entered by the user when resolving a DUPLICATE or OTHER item.
   * Also used as supplemental note for DOCUMENT items.
   */
  private String explanation;

  /**
   * Back-reference to the ValidationFinding this item was created from.
   * Null for synthetic items (e.g. LLM-generated ClarificationItemList, fallback items).
   */
  private ValidationFinding finding;

  /** True once the user has acted on this item (uploaded doc or submitted explanation). */
  private boolean resolved;

  public ClarificationItem() {
  }

  public ClarificationItem(String message, ClarificationProblemType problemType, String documentTypeKey) {
    this.message = message;
    this.problemType = problemType;
    this.documentTypeKey = documentTypeKey;
  }

  public ClarificationItem(String message, ClarificationProblemType problemType, String documentTypeKey, ValidationFinding finding) {
    this.message = message;
    this.problemType = problemType;
    this.documentTypeKey = documentTypeKey;
    this.finding = finding;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ClarificationProblemType getProblemType() {
    return problemType;
  }

  public void setProblemType(ClarificationProblemType problemType) {
    this.problemType = problemType;
  }

  public String getDocumentTypeKey() {
    return documentTypeKey;
  }

  public void setDocumentTypeKey(String documentTypeKey) {
    this.documentTypeKey = documentTypeKey;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public boolean isResolved() {
    return resolved;
  }

  public void setResolved(boolean resolved) {
    this.resolved = resolved;
  }

  public ValidationFinding getFinding() {
    return finding;
  }

  public void setFinding(ValidationFinding finding) {
    this.finding = finding;
  }
}
