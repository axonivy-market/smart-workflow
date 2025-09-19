package com.axonivy.utils.smart.orchestrator.demo.shopping.productcategory;

import org.apache.commons.lang3.StringUtils;

import dev.langchain4j.internal.Json;
import dev.langchain4j.model.output.structured.Description;

public class ProductCategorySearchCriteria {

  @Description("Unique category identifier. Leave null if no filter is needed.")
  private String categoryId;

  @Description("Category name filter (case-insensitive partial match). Leave null if not filtering by category name.")
  private String nameContains;

  @Description("Category description filter (case-insensitive partial match). Leave null if not filtering by description.")
  private String descriptionContains;

  public ProductCategorySearchCriteria() {
  }

  // Getters and setters
  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getNameContains() {
    return nameContains;
  }

  public void setNameContains(String nameContains) {
    this.nameContains = nameContains;
  }

  public String getDescriptionContains() {
    return descriptionContains;
  }

  public void setDescriptionContains(String descriptionContains) {
    this.descriptionContains = descriptionContains;
  }

  /**
   * Checks if this search criteria has any filters set.
   */
  public boolean hasAnyFilter() {
    return StringUtils.isNotBlank(categoryId) || StringUtils.isNotBlank(nameContains)
        || StringUtils.isNotBlank(descriptionContains);
  }

  @Override
  public String toString() {
    return Json.toJson(this);
  }
}