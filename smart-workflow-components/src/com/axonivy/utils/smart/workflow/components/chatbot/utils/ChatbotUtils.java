package com.axonivy.utils.smart.workflow.components.chatbot.utils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage;

import ch.ivyteam.ivy.bpm.error.BpmError;
import tools.jackson.databind.json.JsonMapper;

public final class ChatbotUtils {
  /** Only the most recent messages are sent to the AI, to bound the size and cost of each request. */
  static final int MAX_HISTORY_MESSAGES = 20;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ChatbotUtils() {
  }

  record Turn(String role, String content) {}

  record AgentQuery(List<Turn> chatHistory, String latestUserMessage) {}

  /**
   * The query sent to the agent: one JSON object holding the earlier turns and the message to answer.
   * JSON escaping keeps user text inside its "content" string, so it cannot pose as an AI turn, and the
   * result is a single line, which keeps the regex prompt-injection guardrail from rejecting line breaks.
   */
  public static String toAgentQuery(String userMessage, List<SmartWorkflowMessage> chatHistory) {
    List<SmartWorkflowMessage> history = chatHistory == null ? List.of() : chatHistory;
    List<Turn> turns = history.subList(Math.max(0, history.size() - MAX_HISTORY_MESSAGES), history.size())
        .stream()
        .map(message -> new Turn(message.getRole().name().toLowerCase(Locale.ROOT), message.getContent()))
        .toList();
    return JSON.writeValueAsString(new AgentQuery(turns, userMessage));
  }

  /**
   * Text for the "details" part of an error message: the error code and id of a {@link BpmError},
   * followed by the full stack trace.
   */
  public static String errorDetails(Throwable error) {
    if (error == null) {
      return "";
    }
    StringBuilder details = new StringBuilder();
    if (error instanceof BpmError bpmError) {
      details.append("Error code: ").append(bpmError.getErrorCode()).append('\n');
      details.append("Error id: ").append(bpmError.getId()).append("\n\n");
    }
    StringWriter stackTrace = new StringWriter();
    error.printStackTrace(new PrintWriter(stackTrace));
    return details.append(stackTrace).toString();
  }
}
