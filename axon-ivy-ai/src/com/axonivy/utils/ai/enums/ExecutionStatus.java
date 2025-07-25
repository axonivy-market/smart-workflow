package com.axonivy.utils.ai.enums;

/**
 * Represents the current status of an agent execution.
 */
public enum ExecutionStatus {

  /**
   * The agent execution is currently running and processing steps.
   */
  IN_PROGRESS,

  /**
   * The agent execution is waiting for external input or user interaction.
   */
  PENDING,

  /**
   * The agent execution has completed successfully.
   */
  DONE,

  /**
   * The agent execution has encountered an error and stopped.
   */
  ERROR
}