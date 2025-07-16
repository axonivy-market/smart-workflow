package com.axonivy.utils.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InstructionType {
  EXTRACT_INPUT("input"), EXECUTION("execution"), EXTRACT_OUTPUT("output"), PLANNING("planning");

  private InstructionType(String name) {
    this.name = name;
  }

  private String name;

  @JsonValue
  public String getName() {
    return name;
  }
}
