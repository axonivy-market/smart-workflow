package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.time.LocalDate;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.OnboardingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.langchain4j.model.output.structured.Description;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OnboardingRequest {

  @Description("User who initiated the request")
  private String requestedBy;

  @Description("Department ID from the available departments list, e.g. DEPT-001")
  private String department;

  @Description("Products or services needed from this supplier")
  private String productsServicesNeeded;

  @Description("Expected annual procurement volume in EUR, e.g. 50000")
  private Double expectedAnnualVolume;

  @Description("Request urgency: Normal, High, or Critical")
  private String urgency;

  @Description("Date by which the supplier is needed, format yyyy-MM-dd")
  private LocalDate neededByDate;

  @Description("Additional context or notes for the procurement or QM team")
  private String additionalNotes;

  @Description("The supplier being onboarded")
  private Supplier supplier;

  @Description("Risk score assessment result from the agent")
  private SupplierRiskScore riskScore;

  @Description("Current status of the onboarding workflow")
  private OnboardingStatus status;

  @Description("Existing suppliers matched during the duplicate check step")
  private List<Supplier> matchedSuppliers;

  @Description("Policy validation findings saved from the most recent agent analysis run")
  private List<ValidationFinding> policyValidationFindings;

  @Description("Case UUID from Ivy.wfCase().uuid()")
  private String caseUuid;

  @Description("Completion timestamp in ISO-8601 text format")
  private String completedAt;

  @Description("Human-readable process duration text, e.g. 1h 8min")
  private String processDuration;

  @Description("Decline timestamp in ISO-8601 text format, set only on the RED path")
  private String declinedAt;

  @Description("Full ordered audit trail for this onboarding request")
  private List<AuditTrailEntry> auditTrail;

  public OnboardingRequest() {
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getProductsServicesNeeded() {
    return productsServicesNeeded;
  }

  public void setProductsServicesNeeded(String productsServicesNeeded) {
    this.productsServicesNeeded = productsServicesNeeded;
  }

  public Double getExpectedAnnualVolume() {
    return expectedAnnualVolume;
  }

  public void setExpectedAnnualVolume(Double expectedAnnualVolume) {
    this.expectedAnnualVolume = expectedAnnualVolume;
  }

  public String getUrgency() {
    return urgency;
  }

  public void setUrgency(String urgency) {
    this.urgency = urgency;
  }

  public LocalDate getNeededByDate() {
    return neededByDate;
  }

  public void setNeededByDate(LocalDate neededByDate) {
    this.neededByDate = neededByDate;
  }

  public String getAdditionalNotes() {
    return additionalNotes;
  }

  public void setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public SupplierRiskScore getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(SupplierRiskScore riskScore) {
    this.riskScore = riskScore;
  }

  public OnboardingStatus getStatus() {
    return status;
  }

  public void setStatus(OnboardingStatus status) {
    this.status = status;
  }

  public List<Supplier> getMatchedSuppliers() {
    return matchedSuppliers;
  }

  public void setMatchedSuppliers(List<Supplier> matchedSuppliers) {
    this.matchedSuppliers = matchedSuppliers;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    return policyValidationFindings;
  }

  public void setPolicyValidationFindings(List<ValidationFinding> policyValidationFindings) {
    this.policyValidationFindings = policyValidationFindings;
  }

  public String getCaseUuid() {
    return caseUuid;
  }

  public void setCaseUuid(String caseUuid) {
    this.caseUuid = caseUuid;
  }

  public String getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(String completedAt) {
    this.completedAt = completedAt;
  }

  public String getProcessDuration() {
    return processDuration;
  }

  public void setProcessDuration(String processDuration) {
    this.processDuration = processDuration;
  }

  public String getDeclinedAt() {
    return declinedAt;
  }

  public void setDeclinedAt(String declinedAt) {
    this.declinedAt = declinedAt;
  }

  public List<AuditTrailEntry> getAuditTrail() {
    return auditTrail;
  }

  public void setAuditTrail(List<AuditTrailEntry> auditTrail) {
    this.auditTrail = auditTrail;
  }
}
