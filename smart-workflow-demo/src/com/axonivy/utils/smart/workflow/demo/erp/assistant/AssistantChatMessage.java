package com.axonivy.utils.smart.workflow.demo.erp.assistant;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback.AgentFeedback;

/**
 * Chat message for the AI assistant conversation history.
 *
 * @param role      {@code "user"} or {@code "assistant"}
 * @param content   the message text
 * @param timestamp formatted display timestamp (HH:mm dd-MM-yyyy)
 * @param feedbackList optional per-item feedback (analysis results or alternatives); null for supplier messages
 */
public class AssistantChatMessage implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("HH:mm  dd-MM-yyyy");

  private String role;
  private String content;
  private String timestamp;
  private List<AgentFeedback> feedbackList;

  public AssistantChatMessage(String role, String content) {
    this(role, content, null);
  }

  public AssistantChatMessage(String role, String content, List<AgentFeedback> feedbackList) {
    this.role = role;
    this.content = content;
    this.timestamp = LocalDateTime.now().format(FMT);
    this.feedbackList = feedbackList;
  }

  public boolean isUser() {
    return "user".equals(role);
  }

  public boolean isAssistant() {
    return "assistant".equals(role);
  }

  public String getRole()              { return role; }
  public String getContent()           { return content; }
  public String getTimestamp()         { return timestamp; }
  public List<AgentFeedback> getFeedbackList() { return feedbackList; }
}
