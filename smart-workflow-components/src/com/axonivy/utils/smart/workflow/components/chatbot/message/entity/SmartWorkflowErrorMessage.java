package com.axonivy.utils.smart.workflow.components.chatbot.message.entity;

import java.io.Serializable;
public final class SmartWorkflowErrorMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String content;
  private final boolean retryAllowed;

  public SmartWorkflowErrorMessage(String content) {
    this(content, true);
  }

  public SmartWorkflowErrorMessage(String content, boolean retryAllowed) {
    this.content = content;
    this.retryAllowed = retryAllowed;
  }

  public String getContent() {
    return content;
  }

  public boolean isRetryAllowed() {
    return retryAllowed;
  }
}
