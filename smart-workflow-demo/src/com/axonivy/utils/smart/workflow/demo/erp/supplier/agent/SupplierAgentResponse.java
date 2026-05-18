package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Status;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierSearchCriteria;

import dev.langchain4j.model.output.structured.Description;

public class SupplierAgentResponse {

  @Description("Status of the action")
  private Status status;

  @Description("Feedback after run an action")
  private String feedback;

  @Description("Risk score assessment result from the agent")
  private SupplierRiskScore riskScore;

  @Description("List of validation findings from document extraction and policy checks")
  private List<ValidationFinding> validationFindings;

  @Description("Similarity match score in percent (0-100) for duplicate check results")
  private Integer matchScore;

  @Description("Routing decision based on risk score: APPROVAL, CLARIFICATION, or DECLINE")
  private String routingDecision;

  @Description("Processing steps with timing and log lines from agent tool execution")
  private List<AgentProcessingStep> processingSteps;

  @Description("The supplier related to the action")
  private Supplier supplier;

  @Description("The lsit of suppliers related to the action")
  private List<Supplier> suppliers;

  @Description("Existence status of the supplier.")
  private Boolean isSupplierExisting;

  @Description("Search criteria to find the supplier")
  private SupplierSearchCriteria supplierSearchCriteria;

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public SupplierSearchCriteria getSupplierSearchCriteria() {
    return supplierSearchCriteria;
  }

  public void setSupplierSearchCriteria(SupplierSearchCriteria supplierSearchCriteria) {
    this.supplierSearchCriteria = supplierSearchCriteria;
  }

  public Boolean getIsSupplierExisting() {
    return isSupplierExisting;
  }

  public void setIsSupplierExisting(Boolean isSupplierExisting) {
    this.isSupplierExisting = isSupplierExisting;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getFeedback() {
    return feedback;
  }

  public void setFeedback(String feedback) {
    this.feedback = feedback;
  }

  public List<Supplier> getSuppliers() {
    return suppliers;
  }

  public void setSuppliers(List<Supplier> suppliers) {
    this.suppliers = suppliers;
  }

  public SupplierRiskScore getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(SupplierRiskScore riskScore) {
    this.riskScore = riskScore;
  }

  public List<ValidationFinding> getValidationFindings() {
    return validationFindings;
  }

  public void setValidationFindings(List<ValidationFinding> validationFindings) {
    this.validationFindings = validationFindings;
  }

  public Integer getMatchScore() {
    return matchScore;
  }

  public void setMatchScore(Integer matchScore) {
    this.matchScore = matchScore;
  }

  public String getRoutingDecision() {
    return routingDecision;
  }

  public void setRoutingDecision(String routingDecision) {
    this.routingDecision = routingDecision;
  }

  public List<AgentProcessingStep> getProcessingSteps() {
    return processingSteps;
  }

  public void setProcessingSteps(List<AgentProcessingStep> processingSteps) {
    this.processingSteps = processingSteps;
  }
}