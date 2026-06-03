package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public class AgentProcessingStep implements Serializable {

  private static final long serialVersionUID = 1L;

  private String stepKey;

  @Description("Step title, e.g. 'Document Extraction'")
  private String name;

  @Description("Current status of this processing step: PENDING, RUNNING, COMPLETED, or FAILED")
  private StepStatus status;

  @Description("Timestamp when this step started processing")
  private Instant startedAt;

  @Description("Timestamp when this step finished processing")
  private Instant completedAt;

  @Description("Elapsed time in milliseconds from start to completion")
  private Long durationMs;

  @Description("Detail log lines for this step, each with a severity and message")
  private List<LogLine> logLines;

  public AgentProcessingStep() {
    this.status = StepStatus.PENDING;
    this.logLines = new ArrayList<>();
  }

  public String getStepKey() {
    return stepKey;
  }

  public void setStepKey(String stepKey) {
    this.stepKey = stepKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StepStatus getStatus() {
    return status;
  }

  public void setStatus(StepStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public List<LogLine> getLogLines() {
    return logLines;
  }

  public void setLogLines(List<LogLine> logLines) {
    this.logLines = logLines;
  }

  public enum StepStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String description;

    StepStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  public enum LogLineSeverity {
    OK("Ok"),
    WARNING("Warning"),
    ERROR("Error");

    private final String description;

    LogLineSeverity(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  public static class LogLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Description("Severity of this log line: OK, WARNING, or ERROR — maps to check/warn/error icon in the UI")
    private LogLineSeverity severity;

    @Description("Human-readable description of what happened at this step detail")
    private String message;

    private boolean italic;

    public LogLine() {
    }

    public LogLine(LogLineSeverity severity, String message) {
      this.severity = severity;
      this.message = message;
    }

    public LogLine(LogLineSeverity severity, String message, boolean italic) {
      this.severity = severity;
      this.message = message;
      this.italic = italic;
    }

    public LogLineSeverity getSeverity() {
      return severity;
    }

    public void setSeverity(LogLineSeverity severity) {
      this.severity = severity;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public boolean isItalic() {
      return italic;
    }

    public void setItalic(boolean italic) {
      this.italic = italic;
    }
  }
}
