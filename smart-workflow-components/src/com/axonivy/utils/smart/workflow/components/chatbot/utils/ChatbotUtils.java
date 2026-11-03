package com.axonivy.utils.smart.workflow.components.chatbot.utils;
import java.util.List;
import java.util.Locale;

import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage;

import tools.jackson.databind.json.JsonMapper;

public final class ChatbotUtils {
  static final int MAX_HISTORY_MESSAGES = 20;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ChatbotUtils() {
  }

  record Turn(String role, String content) {}

  record AgentQuery(List<Turn> chatHistory, String latestUserMessage) {}

  public static String toAgentQuery(String userMessage, List<SmartWorkflowMessage> chatHistory) {
    List<SmartWorkflowMessage> history = chatHistory == null ? List.of() : chatHistory;
    List<Turn> turns = history.subList(Math.max(0, history.size() - MAX_HISTORY_MESSAGES), history.size())
        .stream()
        .map(message -> new Turn(message.getRole().name().toLowerCase(Locale.ROOT), message.getContent()))
        .toList();
    return JSON.writeValueAsString(new AgentQuery(turns, userMessage));
  }
}
