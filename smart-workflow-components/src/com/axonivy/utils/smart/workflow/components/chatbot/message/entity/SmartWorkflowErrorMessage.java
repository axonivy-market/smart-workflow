package com.axonivy.utils.smart.workflow.components.chatbot.message.entity;

import java.io.Serializable;

/**
 * An error shown in the chat instead of an answer: a short message for the user plus technical
 * details (error code and stack trace). It is displayed below the conversation and is not a
 * {@link SmartWorkflowMessage}, so it can never end up in the history sent to the AI.
 */
public final class SmartWorkflowErrorMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String content;
  private final String details;
  private final boolean retryable;

  public SmartWorkflowErrorMessage(String content, String details) {
    this(content, details, true);
  }

  /** @param retryable false when asking again cannot help, e.g. the message itself was rejected */
  public SmartWorkflowErrorMessage(String content, String details, boolean retryable) {
    this.content = content;
    this.details = details;
    this.retryable = retryable;
  }

  public String getContent() {
    return content;
  }

  public String getDetails() {
    return details;
  }

  public boolean hasDetails() {
    return details != null && !details.isBlank();
  }

  public boolean isRetryable() {
    return retryable;
  }
}
