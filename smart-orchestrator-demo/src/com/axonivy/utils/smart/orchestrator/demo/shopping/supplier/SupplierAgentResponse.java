package com.axonivy.utils.smart.orchestrator.demo.shopping.supplier;

import com.axonivy.utils.smart.orchestrator.demo.shopping.enums.Status;

import dev.langchain4j.model.output.structured.Description;

public class SupplierAgentResponse {

  @Description("Status of the action")
  private Status status;

  @Description("Feedback after run an action")
  private String feedback;

  @Description("The supplier related to the action")
  private Supplier supplier;

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
}