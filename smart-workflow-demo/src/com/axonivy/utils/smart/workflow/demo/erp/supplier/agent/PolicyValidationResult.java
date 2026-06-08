package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class PolicyValidationResult implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("List of validation findings produced by the policy check — each classified as PASSED, WARNING, or FAILURE")
  private List<ValidationFinding> findings;

  @Description("The document extraction result that was used as input for the policy validation — useful for traceability and debugging, but not required for the actual policy check logic")
  private DocumentExtractionResult documentExtractionResult;

  @JsonIgnore
  private AgentProcessingStep processingStep;

  @JsonIgnore
  private List<SupplierPolicyRule> ruleEvaluations;

  @JsonIgnore
  private int complianceScore;

  public PolicyValidationResult() {
    this.findings = new ArrayList<>();
  }

  public List<ValidationFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<ValidationFinding> findings) {
    this.findings = findings;
  }

  public AgentProcessingStep getProcessingStep() {
    return processingStep;
  }

  public void setProcessingStep(AgentProcessingStep processingStep) {
    this.processingStep = processingStep;
  }

  public List<SupplierPolicyRule> getRuleEvaluations() {
    return ruleEvaluations;
  }

  public void setRuleEvaluations(List<SupplierPolicyRule> ruleEvaluations) {
    this.ruleEvaluations = ruleEvaluations;
  }

  public int getComplianceScore() {
    return complianceScore;
  }

  public void setComplianceScore(int complianceScore) {
    this.complianceScore = complianceScore;
  }

  public DocumentExtractionResult getDocumentExtractionResult() {
    return documentExtractionResult;
  }

  public void setDocumentExtractionResult(DocumentExtractionResult documentExtractionResult) {
    this.documentExtractionResult = documentExtractionResult;
  }
}
