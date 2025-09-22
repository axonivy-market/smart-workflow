package com.axonivy.utils.smart.workflow.demo.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Position {
  JUNIOR("junior"), SENIOR("senior"), MANAGER("manager");

  private Position(String name) {
    this.name = name;
  }

  private String name;

  @JsonValue
  public String getName() {
    return this.name;
  }
}
