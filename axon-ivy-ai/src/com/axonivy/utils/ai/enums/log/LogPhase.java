package com.axonivy.utils.ai.enums.log;

/**
 * Defines the different phases of activity execution lifecycle. Phases help
 * track the progression of operations from start to completion.
 */
public enum LogPhase {

  /**
   * Activity is being initialized or started
   */
  INIT,

  /**
   * Activity is currently in progress or running
   */
  RUNNING,

  /**
   * Activity has completed successfully
   */
  COMPLETE,

  /**
   * Activity has encountered an error or failed
   */
  ERROR,

  /**
   * Activity has been paused or suspended
   */
  PAUSED,

  /**
   * Activity has been cancelled or aborted
   */
  CANCELLED,

  /**
   * Activity is being retried after a previous attempt
   */
  RETRY
}