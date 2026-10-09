package com.axonivy.utils.smart.workflow.model.spi;

import java.util.List;

import dev.langchain4j.model.chat.listener.ChatModelListener;

public record ModelOptions(
    String modelName,
    boolean structuredOutput,
    boolean hasTools,
    List<ChatModelListener> listeners) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String modelName;
    private boolean structuredOutput;
    private boolean hasTools;
    private List<ChatModelListener> listeners = List.of();

    public Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public Builder structuredOutput(boolean structuredOutput) {
      this.structuredOutput = structuredOutput;
      return this;
    }

    public Builder hasTools(boolean hasTools) {
      this.hasTools = hasTools;
      return this;
    }

    public Builder listeners(List<ChatModelListener> listeners) {
      this.listeners = listeners;
      return this;
    }

    public ModelOptions build() {
      return new ModelOptions(modelName, structuredOutput, hasTools, listeners);
    }
  }
}
