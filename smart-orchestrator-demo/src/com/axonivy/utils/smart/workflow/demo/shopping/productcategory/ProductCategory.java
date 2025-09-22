package com.axonivy.utils.smart.workflow.demo.shopping.productcategory;

import dev.langchain4j.model.output.structured.Description;

public class ProductCategory {

  @Description("Unique category identifier")
  private String categoryId;

  @Description("Name of the product category")
  private String name;

  @Description("Detailed description of the product category")
  private String description;

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}