package com.axonivy.utils.smart.workflow.exception;

public class SmartWorkflowException extends RuntimeException {
  public SmartWorkflowException(String message, Throwable cause) {
    super(message, cause);
  }
  public SmartWorkflowException(String message) {
    super(message);
  }
}
