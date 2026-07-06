package com.axonivy.utils.smart.workflow.demo.assistant;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AssistantChatMessageFactory {

  private AssistantChatMessageFactory() {}

  public static AssistantChatMessage of(String role, String content) {
    AssistantChatMessage msg = new AssistantChatMessage();
    msg.setRole(role);
    msg.setContent(content);
    msg.setTimestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    return msg;
  }
}
