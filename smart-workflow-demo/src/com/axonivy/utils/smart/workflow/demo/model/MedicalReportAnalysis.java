package com.axonivy.utils.smart.workflow.demo.model;

import dev.langchain4j.model.output.structured.Description;

@Description("Comprehensive analysis of a medical report")
public class MedicalReportAnalysis {

  @Description("Extracted patient vitals: blood pressure, heart rate, temperature, weight, BMI")
  private String vitals;

  @Description("Abnormal readings identified and any urgent clinical concerns")
  private String abnormalities;

  @Description("Concise clinical summary suitable for a physician review")
  private String clinicalSummary;

  @Description("Overall risk level: low, moderate, or high")
  private String riskLevel;

  public String getVitals() {
    return vitals;
  }

  public void setVitals(String vitals) {
    this.vitals = vitals;
  }

  public String getAbnormalities() {
    return abnormalities;
  }

  public void setAbnormalities(String abnormalities) {
    this.abnormalities = abnormalities;
  }

  public String getClinicalSummary() {
    return clinicalSummary;
  }

  public void setClinicalSummary(String clinicalSummary) {
    this.clinicalSummary = clinicalSummary;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }
}
