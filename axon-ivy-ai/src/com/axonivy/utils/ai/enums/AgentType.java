package com.axonivy.utils.ai.enums;

/**
 * Enum representing different types of AI agents available in the system.
 */
public enum AgentType {
  
  /**
   * Step-by-step agent that executes a planned sequence of steps.
   * This is the current implementation approach.
   */
  STEP_BY_STEP("IvyAgent"),
  
  /**
   * Todo-based agent that works through a list of todos until each is completed.
   * This agent focuses on achieving specific outcomes rather than following predetermined steps.
   */
  TODO_LIST("TodoAgent");
  
  private final String className;
  
  AgentType(String className) {
    this.className = className;
  }
  
  public String getClassName() {
    return className;
  }
  
  /**
   * Get AgentType by class name
   */
  public static AgentType fromClassName(String className) {
    for (AgentType type : values()) {
      if (type.getClassName().equals(className)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown agent class name: " + className);
  }
} 