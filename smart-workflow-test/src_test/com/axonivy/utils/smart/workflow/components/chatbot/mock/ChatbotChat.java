package com.axonivy.utils.smart.workflow.components.chatbot.mock;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.ai.mock.MockOpenAI;

import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Answers every chat completion with a fixed assistant text and records the received requests.
 */
public class ChatbotChat {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final String answer;
  private final List<JsonNode> requests = new ArrayList<>();

  private ChatbotChat(String answer) {
    this.answer = answer;
  }

  public static ChatbotChat answering(String answer) {
    var chat = new ChatbotChat(answer);
    MockOpenAI.defineChat(chat::respond);
    return chat;
  }

  public List<JsonNode> requests() {
    return requests;
  }

  /**
   * @return the content of the last user message the AI received
   */
  public String lastUserMessage() {
    var messages = requests.getLast().get("messages");
    for (int i = messages.size() - 1; i >= 0; i--) {
      if ("user".equals(messages.get(i).get("role").asString())) {
        return messages.get(i).get("content").asString();
      }
    }
    throw new IllegalStateException("No user message in request: " + requests.getLast());
  }

  private Response respond(JsonNode request) {
    requests.add(request);
    return Response.ok(completion(answer).toString()).build();
  }

  private static ObjectNode completion(String content) {
    ObjectNode response = JSON.createObjectNode()
        .put("id", "chatcmpl-chatbot")
        .put("object", "chat.completion")
        .put("created", 1755166389)
        .put("model", "gpt-4.1-mini");
    ObjectNode choice = response.putArray("choices").addObject()
        .put("index", 0)
        .put("finish_reason", "stop");
    choice.putObject("message")
        .put("role", "assistant")
        .put("content", content);
    response.putObject("usage")
        .put("prompt_tokens", 20)
        .put("completion_tokens", 10)
        .put("total_tokens", 30);
    return response;
  }
}
