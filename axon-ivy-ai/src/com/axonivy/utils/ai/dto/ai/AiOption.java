package com.axonivy.utils.ai.dto.ai;

public class AiOption {
  private String id;
  private String condition;

  public AiOption(String id, String condition) {
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
