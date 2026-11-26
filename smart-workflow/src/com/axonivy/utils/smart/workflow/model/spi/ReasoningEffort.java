package com.axonivy.utils.smart.workflow.model.spi;

public enum ReasoningEffort {
  NONE("none"),
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high");

  private final String text;

  ReasoningEffort(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
