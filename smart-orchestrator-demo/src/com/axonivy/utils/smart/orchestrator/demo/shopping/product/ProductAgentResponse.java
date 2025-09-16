package com.axonivy.utils.smart.orchestrator.demo.shopping.product;

import com.axonivy.utils.smart.orchestrator.demo.shopping.enums.Status;

import dev.langchain4j.model.output.structured.Description;

public class ProductAgentResponse {

  @Description("Status of the action")
  private Status status;

  @Description("Feedback after run an action")
  private String feedback;

  @Description("The product related to the action")
  private Product product;

  @Description("Search criteria to find the product")
  private ProductSearchCriteria productSearchCriteria;

  @Description("Existence status of the product.")
  private Boolean isProductExisting;

  @Description("Existence status of the supplier.")
  private Boolean isSupplierExisting;

  @Description("Existence status of the product category.")
  private Boolean isCategoryExisting;

  @Description("Existence status of the product brand.")
  private Boolean isBrandExisting;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public ProductSearchCriteria getProductSearchCriteria() {
    return productSearchCriteria;
  }

  public void setProductSearchCriteria(ProductSearchCriteria productSearchCriteria) {
    this.productSearchCriteria = productSearchCriteria;
  }

  public Boolean getIsProductExisting() {
    return isProductExisting;
  }

  public void setIsProductExisting(Boolean isProductExisting) {
    this.isProductExisting = isProductExisting;
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

  public Boolean getIsSupplierExisting() {
    return isSupplierExisting;
  }

  public void setIsSupplierExisting(Boolean isSupplierExisting) {
    this.isSupplierExisting = isSupplierExisting;
  }

  public Boolean getIsCategoryExisting() {
    return isCategoryExisting;
  }

  public void setIsCategoryExisting(Boolean isCategoryExisting) {
    this.isCategoryExisting = isCategoryExisting;
  }

  public Boolean getIsBrandExisting() {
    return isBrandExisting;
  }

  public void setIsBrandExisting(Boolean isBrandExisting) {
    this.isBrandExisting = isBrandExisting;
  }
}