package com.axonivy.utils.smart.orchestrator.demo.shopping.productcategory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class ProductCategory {

  @Description("Unique category identifier")
  private String categoryId;

  @Description("Name of the product category")
  private String name;

  @Description("Detailed description of the product category")
  private String description;

  @JsonIgnore
  private Double matchingScore;

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

  public Double getMatchingScore() {
    return matchingScore;
  }

  public void setMatchingScore(Double matchingScore) {
    this.matchingScore = matchingScore;
  }
}