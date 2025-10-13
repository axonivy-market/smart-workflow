package com.axonivy.utils.smart.workflow.demo.shopping.common;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public class KeywordPool {
  @Description("a list of relevant keywords of the product")
  private List<String> keywords;

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }
}