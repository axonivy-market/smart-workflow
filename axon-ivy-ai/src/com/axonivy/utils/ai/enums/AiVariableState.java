package com.axonivy.utils.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiVariableState {
  SUCCESS("success"), ERROR("error"), EMPTY("empty");

  String name;

  private AiVariableState(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }
}
