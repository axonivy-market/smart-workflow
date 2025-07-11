package com.axonivy.utils.ai.dto.ai;

public class FieldExplanation {
  private String name;
  private String explanation;

  public FieldExplanation(String name, String explanation) {
    this.name = name;
    this.explanation = explanation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }
}
