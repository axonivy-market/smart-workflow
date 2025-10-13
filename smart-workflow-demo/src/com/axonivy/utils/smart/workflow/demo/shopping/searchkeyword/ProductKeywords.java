package com.axonivy.utils.smart.workflow.demo.shopping.searchkeyword;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public class ProductKeywords {
  @Description("Unique product identifier")
  private String productId;

  @Description("a list of relevant keywords of the product")
  private List<String> keywords = new ArrayList<>();

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }
}
