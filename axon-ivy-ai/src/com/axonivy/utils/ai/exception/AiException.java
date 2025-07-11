package com.axonivy.utils.ai.exception;

public class AiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AiException(String message, Throwable cause) {
    super(message, cause);
  }

  public AiException(String message) {
    super(message);
  }

  public AiException(Throwable cause) {
    super(cause);
  }

}
