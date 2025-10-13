package com.axonivy.utils.smart.workflow.demo.shopping.store;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.shopping.enums.CustomerRequestType;

import dev.langchain4j.model.output.structured.Description;

public class CustomerRequest {

  @Description("Original user query")
  private String originalQuery;

  @Description("Translated user query")
  private String translatedQuery;

  @Description("ID of product categories related to this query")
  private List<String> relatedCategoryIds;

  @Description("Type of request")
  private CustomerRequestType requestType;

  public String getOriginalQuery() {
    return originalQuery;
  }

  public void setOriginalQuery(String originalQuery) {
    this.originalQuery = originalQuery;
  }

  public String getTranslatedQuery() {
    return translatedQuery;
  }

  public void setTranslatedQuery(String translatedQuery) {
    this.translatedQuery = translatedQuery;
  }

  public List<String> getRelatedCategoryIds() {
    return relatedCategoryIds;
  }

  public void setRelatedCategoryIds(List<String> relatedCategoryIds) {
    this.relatedCategoryIds = relatedCategoryIds;
  }

  public CustomerRequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(CustomerRequestType requestType) {
    this.requestType = requestType;
  }
}
