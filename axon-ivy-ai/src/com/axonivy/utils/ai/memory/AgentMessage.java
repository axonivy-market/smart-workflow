package com.axonivy.utils.ai.memory;

import java.util.HashMap;
import java.util.Map;

import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;

import dev.langchain4j.data.message.CustomMessage;

public class AgentMessage extends CustomMessage {

  public AgentMessage(Map<String, Object> attributes) {
    super(attributes);
  }

  // Unique identifier for this log entry
  private String id;

  private LogLevel level; // PLANNING | OBSERVATION | EXECUTION | UPDATE
  private LogPhase phase; // INIT | RUNNING | COMPLETE | ERROR | etc.

  // Content and context
  private String content; // Main log message or description
  private String executionContext; // Step number, tool ID, iteration, etc.

  // Execution hierarchy context
  private String executionId; // AgentExecution ID this belongs to
  private Integer stepNumber; // Step number if applicable
  private String toolId; // Tool ID if applicable
  private Integer iteration; // Iteration number if applicable

  public static Builder getBuilder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LogLevel getLevel() {
    return level;
  }

  public void setLevel(LogLevel level) {
    this.level = level;
  }

  public LogPhase getPhase() {
    return phase;
  }

  public void setPhase(LogPhase phase) {
    this.phase = phase;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getExecutionContext() {
    return executionContext;
  }

  public void setExecutionContext(String executionContext) {
    this.executionContext = executionContext;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public Integer getStepNumber() {
    return stepNumber;
  }

  public void setStepNumber(Integer stepNumber) {
    this.stepNumber = stepNumber;
  }

  public String getToolId() {
    return toolId;
  }

  public void setToolId(String toolId) {
    this.toolId = toolId;
  }

  public Integer getIteration() {
    return iteration;
  }

  public void setIteration(Integer iteration) {
    this.iteration = iteration;
  }

  /**
   * Returns a builder instance to construct LogEntry using the builder pattern.
   */
  public static class Builder {
    private String id;
    private LogLevel level;
    private LogPhase phase;
    private String content;
    private String executionContext;
    private String executionId;
    private Integer stepNumber;
    private String toolId;
    private Integer iteration;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder level(LogLevel level) {
      this.level = level;
      return this;
    }

    public Builder phase(LogPhase phase) {
      this.phase = phase;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }

    public Builder executionContext(String executionContext) {
      this.executionContext = executionContext;
      return this;
    }

    public Builder executionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public Builder stepNumber(Integer stepNumber) {
      this.stepNumber = stepNumber;
      return this;
    }

    public Builder toolId(String toolId) {
      this.toolId = toolId;
      return this;
    }

    public Builder iteration(Integer iteration) {
      this.iteration = iteration;
      return this;
    }

    /**
     * Constructs the AgentMessage instance and assigns a unique ID.
     */
    public AgentMessage build() {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("id", id);
      attributes.put("level", level);
      attributes.put("phase", phase);
      attributes.put("content", content);
      attributes.put("executionContext", executionContext);
      attributes.put("executionId", executionId);
      attributes.put("stepNumber", stepNumber);
      attributes.put("toolId", toolId);
      attributes.put("iteration", iteration);

      return new AgentMessage(attributes);
    }
  }
}
