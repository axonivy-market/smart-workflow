package com.axonivy.utils.ai.dto;

public class Option {
  private String id;
  private String condition;

  public Option() {}

  public Option(String id, String condition) {
    this.id = id;
    this.condition = condition;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }
}