package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.io.Serializable;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Output of the calculateRiskScore callable sub tool.
 * Contains the 4-component risk score, routing decision, and processing step metadata.
 */
public class RiskScoreResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private SupplierRiskScore riskScore;
  private String routingDecision;
  @JsonIgnore
  private AgentProcessingStep processingStep;

  public RiskScoreResult() {
  }

  public SupplierRiskScore getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(SupplierRiskScore riskScore) {
    this.riskScore = riskScore;
  }

  public String getRoutingDecision() {
    return routingDecision;
  }

  public void setRoutingDecision(String routingDecision) {
    this.routingDecision = routingDecision;
  }

  public AgentProcessingStep getProcessingStep() {
    return processingStep;
  }

  public void setProcessingStep(AgentProcessingStep processingStep) {
    this.processingStep = processingStep;
  }
}
