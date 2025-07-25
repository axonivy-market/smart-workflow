package com.axonivy.utils.ai.enums.log;

/**
 * Defines the main levels of multi-dimensional logging for AI agent execution.
 * Each level represents a different granularity and focus area of the execution
 * process.
 */
public enum LogLevel {

  /**
   * High-level planning activities: goal setting, strategy formation, plan
   * generation
   */
  PLANNING,

  /**
   * Observation and reasoning activities: environmental feedback, ReAct
   * reasoning, progress assessment
   */
  OBSERVATION,

  /**
   * Execution at the step level
   */
  STEP,

  /**
   * Execution at the tool level
   */
  TOOL,

  /**
   * Plan and step modification activities: adaptations, reassignments, flow
   * changes
   */
  UPDATE
}