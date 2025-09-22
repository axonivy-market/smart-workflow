package com.axonivy.utils.smart.workflow.demo.shopping.brand;

import com.axonivy.utils.smart.workflow.demo.shopping.enums.Status;

import dev.langchain4j.model.output.structured.Description;

public class BrandAgentResponse {

  @Description("Status of the action")
  private Status status;

  @Description("Feedback after run an action")
  private String feedback;

  @Description("The brand related to the action")
  private Brand brand;

  @Description("Existence status of the brand.")
  private Boolean isBrandExisting;

  @Description("Search criteria to find the brand")
  private BrandSearchCriteria brandSearchCriteria;

  public Brand getBrand() {
    return brand;
  }

  public void setBrand(Brand brand) {
    this.brand = brand;
  }

  public BrandSearchCriteria getBrandSearchCriteria() {
    return brandSearchCriteria;
  }

  public void setBrandSearchCriteria(BrandSearchCriteria brandSearchCriteria) {
    this.brandSearchCriteria = brandSearchCriteria;
  }

  public Boolean getIsBrandExisting() {
    return isBrandExisting;
  }

  public void setIsBrandExisting(Boolean isBrandExisting) {
    this.isBrandExisting = isBrandExisting;
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