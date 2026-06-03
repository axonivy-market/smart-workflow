package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ClarificationProblemType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;

import dev.langchain4j.model.output.structured.Description;


public class ValidationFinding {

  @Description("Finding severity: PASSED, WARNING, or FAILURE")
  private FindingSeverity severity;

  @Description("Description of the finding")
  private String message;

  @Description("Source of the check, e.g. 'RAG policy', 'cross-reference check'")
  private String source;

  @Description("If this finding requires the supplier to upload a document or certificate, provide a key identifying what to upload. " +
      "Use CERTIFICATION:<NAME> for any certificate type (e.g. CERTIFICATION:ISO_9001, CERTIFICATION:BRC_FOOD, CERTIFICATION:OHSAS_18001). " +
      "Use DOCUMENT:<NAME> for any other document (e.g. DOCUMENT:ANNUAL_REPORT, DOCUMENT:BANK_STATEMENT, DOCUMENT:COMMERCIAL_REGISTER). " +
      "Derive NAME freely from context — do NOT limit to any fixed list. " +
      "Leave null only for non-document findings such as duplicate checks or general data issues.")
  private String documentTypeKey;

  @Description("Points deducted from the compliance score by this finding. " +
      "0 for PASSED, half the rule deduction for WARNING, full rule deduction for FAILURE.")
  private int score;

  @Description("The type of risk this finding relates to: FINANCIAL_STABILITY, POLICY_COMPLIANCE, or CERTIFICATION_VALIDITY")
  private RiskType riskType;

  @Description("The kind of check that produced this finding: MISSING_DOC or AI_VALIDATION")
  private RiskKind riskKind;

  /** Runtime-only: true once the supplier has addressed this finding on the Clarification task. Not part of LLM extraction. */
  private boolean resolved;

  /** AI-generated explanation or context for this finding, populated by the validation agent. Not part of LLM extraction. */
  private String aiExplanation;

  /** Runtime-only: explanation text entered by the user for DUPLICATE or OTHER type findings on the Clarification task. Not part of LLM extraction. */
  private String userExplanation;

  public ValidationFinding() {
  }

  public ValidationFinding(FindingSeverity severity, String message, String source, RiskType riskType) {
    this.severity = severity;
    this.message = message;
    this.source = source;
    this.riskType = riskType;
  }

  public FindingSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(FindingSeverity severity) {
    this.severity = severity;
  }

  private FindingSeverity effectiveSeverity() {
    return severity != null ? severity : FindingSeverity.PASSED;
  }

  public String getRowClass() {
    return effectiveSeverity().rowClass;
  }

  public String getIcon() {
    return effectiveSeverity().icon;
  }

  public String getBadgeClass() {
    return effectiveSeverity().badgeClass;
  }

  public String getLogClass() {
    return effectiveSeverity().logClass;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getDocumentTypeKey() {
    return documentTypeKey;
  }

  public void setDocumentTypeKey(String documentTypeKey) {
    this.documentTypeKey = documentTypeKey;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public RiskType getRiskType() {
    return riskType;
  }

  public void setRiskType(RiskType riskType) {
    this.riskType = riskType;
  }

  public RiskKind getRiskKind() {
    return riskKind;
  }

  public void setRiskKind(RiskKind riskKind) {
    this.riskKind = riskKind;
  }

  public boolean isResolved() {
    return resolved;
  }

  public void setResolved(boolean resolved) {
    this.resolved = resolved;
  }

  public String getAiExplanation() {
    return aiExplanation;
  }

  public void setAiExplanation(String aiExplanation) {
    this.aiExplanation = aiExplanation;
  }

  public String getUserExplanation() {
    return userExplanation;
  }

  public void setUserExplanation(String userExplanation) {
    this.userExplanation = userExplanation;
  }

  public ClarificationProblemType getProblemType() {
    return ClarificationProblemTypeBuilder.resolve(documentTypeKey, riskKind, source, message);
  }
}
