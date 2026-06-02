package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class CrossReferenceResult implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("List of cross-reference check findings: company register, VAT ID, ERP duplicate, and sanctions screening")
  private List<ValidationFinding> findings;

  @JsonIgnore
  private AgentProcessingStep processingStep;

  public CrossReferenceResult() {
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
}
