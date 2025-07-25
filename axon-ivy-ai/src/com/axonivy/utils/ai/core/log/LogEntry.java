package com.axonivy.utils.ai.core.log;

import java.io.Serializable;

import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.utils.IdGenerationUtils;

/**
 * Represents a single entry in the multi-dimensional logging system. Contains
 * rich metadata, causality relationships, and execution context.
 */
public class LogEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  // Unique identifier for this log entry
  private String id;

  // Core dimensions
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

  /**
   * Returns a builder instance to construct LogEntry.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a builder instance to construct LogEntry using the builder pattern.
   */
  public static class Builder {
    private LogLevel level;
    private LogPhase phase;
    private String content;
    private String executionContext;
    private String executionId;
    private Integer stepNumber;
    private String toolId;
    private Integer iteration;

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
     * Constructs the LogEntry instance and assigns a unique ID.
     */
    public LogEntry build() {
      LogEntry entry = new LogEntry();
      entry.id = IdGenerationUtils.generateRandomId(); // Auto-generate unique ID
      entry.level = this.level;
      entry.phase = this.phase;
      entry.content = this.content;
      entry.executionContext = this.executionContext;
      entry.executionId = this.executionId;
      entry.stepNumber = this.stepNumber;
      entry.toolId = this.toolId;
      entry.iteration = this.iteration;
      return entry;
    }
  }

  /**
   * Default constructor
   */
  public LogEntry() {
    this.id = IdGenerationUtils.generateRandomId();
  }

  /**
   * Constructor with core dimensions
   */
  public LogEntry(LogLevel level, LogPhase phase, String content) {
    this();
    this.level = level;
    this.phase = phase;
    this.content = content;
  }

  /**
   * Constructor with full context
   */
  public LogEntry(LogLevel level, LogPhase phase, String content, String executionContext,
      String executionId) {
    this(level, phase, content);
    this.executionContext = executionContext;
    this.executionId = executionId;
  }

  /**
   * Sets step context
   */
  public LogEntry withStepContext(Integer stepNumber, String toolId) {
    this.stepNumber = stepNumber;
    this.toolId = toolId;
    return this;
  }

  /**
   * Sets iteration context
   */
  public LogEntry withIteration(Integer iteration) {
    this.iteration = iteration;
    return this;
  }

  /**
   * Generates a human-readable string representation
   */
  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s/%s", level, phase));

    if (executionContext != null) {
      sb.append(" (").append(executionContext).append(")");
    }

    sb.append(": ").append(content);

    return sb.toString();
  }

  /**
   * Generates a compact string for debugging
   */
  public String toCompactString() {
    return String.format("%s/%s/%s: %s", level, phase, content);
  }

  // Getters and setters
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
}