package com.axonivy.utils.smart.workflow.demo.erp.assistant;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable chat message for the AI assistant conversation history.
 *
 * <p>Records are used instead of an inner class so this type can be shared
 * across any {@link AssistantUploadSupport} implementor without coupling to a
 * specific managed bean.
 *
 * @param role      {@code "user"} or {@code "assistant"}
 * @param content   the message text
 * @param timestamp formatted display timestamp (HH:mm dd-MM-yyyy)
 */
public record AssistantChatMessage(String role, String content, String timestamp)
    implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("HH:mm  dd-MM-yyyy");

  /**
   * Convenience constructor that auto-assigns the current timestamp.
   */
  public AssistantChatMessage(String role, String content) {
    this(role, content, LocalDateTime.now().format(FMT));
  }

  public boolean isUser() {
    return "user".equals(role);
  }

  public boolean isAssistant() {
    return "assistant".equals(role);
  }

  // JSF/EL requires JavaBeans-style getters (records only expose role(), content(), timestamp())
  public String getRole()      { return role; }
  public String getContent()   { return content; }
  public String getTimestamp() { return timestamp; }
}
