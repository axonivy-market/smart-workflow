package com.axonivy.utils.smart.workflow.demo.shopping.productcategory;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.shopping.enums.Status;

import dev.langchain4j.model.output.structured.Description;

public class ProductCategoryAgentResponse {

  @Description("The product category related to the action")
  private ProductCategory category;

  @Description("Existence status of the product category.")
  private Boolean isCategoryExisting;

  @Description("Search criteria to find the product category")
  private ProductCategorySearchCriteria productCategorySearchCriteria;

  @Description("The list of product categories related to the action")
  private List<ProductCategory> categories;

  @Description("Status of the action")
  private Status status;

  @Description("Feedback after run an action")
  private String feedback;

  public ProductCategory getCategory() {
    return category;
  }

  public void setCategory(ProductCategory category) {
    this.category = category;
  }

  public Boolean getIsCategoryExisting() {
    return isCategoryExisting;
  }

  public void setIsCategoryExisting(Boolean isCategoryExisting) {
    this.isCategoryExisting = isCategoryExisting;
  }

  public ProductCategorySearchCriteria getProductCategorySearchCriteria() {
    return productCategorySearchCriteria;
  }

  public void setProductCategorySearchCriteria(ProductCategorySearchCriteria productCategorySearchCriteria) {
    this.productCategorySearchCriteria = productCategorySearchCriteria;
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

  public List<ProductCategory> getCategories() {
    return categories;
  }

  public void setCategories(List<ProductCategory> categories) {
    this.categories = categories;
  }
}
