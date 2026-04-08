package com.axonivy.utils.smart.workflow.tools.entity;

public record ToolParameter(String name, String description, ParameterType type) {

  public enum ParameterType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    STRING_ARRAY,
    NUMBER_ARRAY
  }

  public static ToolParameter of(String name, String description, ParameterType type) {
    return new ToolParameter(name, description, type);
  }
}
