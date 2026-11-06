package com.axonivy.utils.smart.workflow.components.chatbot.message;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class SmartWorkflowMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final MessageRole role;
  private final String content;
  private final Instant timestamp;

  private SmartWorkflowMessage(MessageRole role, String content) {
    this.role = role;
    this.content = content;
    this.timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  }

  public static SmartWorkflowMessage user(String content) {
    return new SmartWorkflowMessage(MessageRole.USER, content);
  }

  public static SmartWorkflowMessage ai(String content) {
    return new SmartWorkflowMessage(MessageRole.AI, content);
  }

  public MessageRole getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public boolean isUser() {
    return role == MessageRole.USER;
  }
}
