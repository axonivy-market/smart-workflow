package com.axonivy.utils.smart.workflow.components.chatbot.message.entity;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.axonivy.utils.smart.workflow.components.chatbot.message.enums.MessageRole;

/** One turn of the conversation, written by the user or the AI. Create it with {@link #user} or {@link #ai}. */
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

  /**
   * ISO-8601 UTC instant, e.g. 2026-11-01T13:05:00Z. The browser formats it in the user's own time zone
   * and locale (see chatbot.js), since the server does not know either.
   */
  public String getTimestampIso() {
    return timestamp.toString();
  }

  public boolean isUser() {
    return role == MessageRole.USER;
  }

  @Override
  public String toString() {
    return role.name() + ": " + content;
  }
}
