package com.axonivy.utils.smart.workflow.demo.shopping.store;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public class SearchResponse {
  @Description("List of category ID")
  private List<String> categoryIds;

  @Description("List of brand ID")
  private List<String> brandIds;

  public List<String> getCategoryIds() {
    return categoryIds;
  }

  public void setCategoryIds(List<String> categoryIds) {
    this.categoryIds = categoryIds;
  }

  public List<String> getBrandIds() {
    return brandIds;
  }

  public void setBrandIds(List<String> brandIds) {
    this.brandIds = brandIds;
  }
}
