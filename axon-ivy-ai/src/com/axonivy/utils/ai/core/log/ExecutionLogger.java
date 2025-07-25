package com.axonivy.utils.ai.core.log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;

public class ExecutionLogger implements Serializable {

  private static final long serialVersionUID = -5449650547249188751L;

  private static final String IVY_VARIABLE = "AI.Logs";

  // Storage for all log entries
  private List<LogEntry> entries;

  // Execution context
  private String executionId;

  /**
   * Constructor
   */
  public ExecutionLogger() {
    this.entries = new ArrayList<>();
  }

  /**
   * Constructor with execution ID
   */
  public ExecutionLogger(String executionId) {
    this();
    this.executionId = executionId;
  }

  /**
   * Main logging method - creates and stores a log entry
   */
  public void log(LogLevel level, LogPhase phase, String content, String executionContext, int iteration) {
    addEntry(LogEntry.builder()
        .executionId(executionId)
        .level(level)
        .phase(phase)
        .content(content)
        .executionContext(executionContext)
        .iteration(iteration).build());
  }

  public void logAdaptivePlan(LogPhase phase, String content, String executionContext, int stepNumber, int iteration,
      String toolId) {
    addEntry(LogEntry.builder()
        .executionId(executionId)
        .level(LogLevel.PLANNING)
        .phase(phase)
        .content(content)
        .executionContext(executionContext)
        .toolId(toolId)
        .iteration(iteration)
        .stepNumber(stepNumber).build());
  }

  /**
   * Adds an entry to the logger
   */
  private void addEntry(LogEntry entry) {
    entries.add(entry);

    // Add log entries to Ivy variable
    addLogEntriesToIvyVariable();
  }

  /**
   * Gets an entry by ID
   */
  public LogEntry getEntry(String id) {
    return entries.stream().filter(entry -> id.equals(entry.getId())).findFirst().orElse(null);
  }

  /**
   * Gets all entries
   */
  public List<LogEntry> getAllEntries() {
    return entries;
  }

  /**
   * Filters entries by level
   */
  public List<LogEntry> getEntriesByLevel(LogLevel level) {
    return entries.stream().filter(entry -> entry.getLevel() == level).collect(Collectors.toList());
  }

  /**
   * Filters entries by phase
   */
  public List<LogEntry> getEntriesByPhase(LogPhase phase) {
    return entries.stream().filter(entry -> entry.getPhase() == phase).collect(Collectors.toList());
  }

  /**
   * Filters entries by step number
   */
  public List<LogEntry> getEntriesByStep(Integer stepNumber) {
    return entries.stream().filter(entry -> stepNumber.equals(entry.getStepNumber()))
        .collect(Collectors.toList());
  }

  /**
   * Filters entries by tool ID
   */
  public List<LogEntry> getEntriesByTool(String toolId) {
    return entries.stream().filter(entry -> toolId.equals(entry.getToolId()))
        .collect(Collectors.toList());
  }

  /**
   * Complex filtering with multiple criteria
   */
  public List<LogEntry> filterEntries(Predicate<LogEntry> filter) {
    return entries.stream().filter(filter).collect(Collectors.toList());
  }

  /**
   * Gets execution summary by level
   */
  public Map<LogLevel, Long> getExecutionSummaryByLevel() {
    return entries.stream().collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
  }

  /**
   * Generates a comprehensive execution report
   */
  public String generateExecutionReport() {
    StringBuilder report = new StringBuilder();
    report.append("=== Multi-Level Execution Report ===\n");
    report.append("Execution ID: ").append(executionId).append("\n");
    report.append("Total Entries: ").append(entries.size()).append("\n\n");

    // Summary by level
    report.append("Summary by Level:\n");
    getExecutionSummaryByLevel()
        .forEach((level, count) -> report.append("  ").append(level).append(": ").append(count).append("\n"));
    report.append("\n");

    // All entries
    report.append("All Entries (").append(entries.size()).append("):\n");
    entries.stream().limit(10)
        .forEach(entry -> report.append("  ").append(entry.toCompactString()).append("\n"));

    return report.toString();
  }

  /**
   * Add all log entries to Ivy variable "AI.Log"
   */
  public void addLogEntriesToIvyVariable() {
    try {
      List<LogEntry> allLogEntries = BusinessEntityConverter.jsonValueToEntities(Ivy.var().get(IVY_VARIABLE),
          LogEntry.class);

      if (allLogEntries == null) {
        allLogEntries = new ArrayList<>();
      }

      // Clear old log entries of the execution
      for (int i = 0; i < allLogEntries.size(); i++) {
        if (allLogEntries.get(i).getExecutionId().equals(executionId)) {
          allLogEntries.remove(i);
          i--;
        }
      }

      // Add log entries of the execution
      allLogEntries.addAll(entries);

      Ivy.var().set(IVY_VARIABLE, BusinessEntityConverter.entityToJsonValue(allLogEntries));
      Ivy.log().debug("Saved " + entries.size() + " log entries to AI.Log variable");
    } catch (Exception e) {
      Ivy.log().error("Failed to save log entries to AI.Log variable: " + e.getMessage(), e);
    }
  }

  /**
   * Loads log entries from Ivy variable "AI.Log"
   */
  public void loadFromIvyVariable() {
    entries.clear();
    entries.addAll(getEntriesFromIvyVariable());
  }

  /**
   * Gets log entries from Ivy variable "AI.Log"
   */
  public List<LogEntry> getEntriesFromIvyVariable() {
    String jsonEntries = Ivy.var().get(IVY_VARIABLE);
    if (StringUtils.isBlank(jsonEntries)) {
      return new ArrayList<>();
    }

    List<LogEntry> foundEntries = BusinessEntityConverter.jsonValueToEntities(jsonEntries, LogEntry.class);
    return foundEntries.stream().filter(entry -> entry.getExecutionId().equals(executionId))
        .collect(Collectors.toList());
  }

  // Getters and setters
  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public int getEntryCount() {
    return entries.size();
  }
}