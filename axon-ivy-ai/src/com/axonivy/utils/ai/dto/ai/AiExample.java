package com.axonivy.utils.ai.dto.ai;

public class AiExample {
  private String query;
  private String expectedResult;

  public AiExample(String query, String expectedResult) {
    this.query = query;
    this.expectedResult = expectedResult;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getExpectedResult() {
    return expectedResult;
  }

  public void setExpectedResult(String expectedResult) {
    this.expectedResult = expectedResult;
  }
}
