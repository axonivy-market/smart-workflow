package com.axonivy.utils.smart.workflow.model.aura.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class AuraChatModel implements ChatModel {

  private static final String ASYNC_NOT_SUPPORTED =
      "Aura accepted the inference request with HTTP 202; asynchronous inference is not implemented yet.";
  private static final String INVALID_RESPONSE =
      "Aura inference response did not contain a string 'content' field.";

  private final List<ChatModelListener> listeners;
  private final WebTarget target;
  private final ChatRequestParameters defaultParameters;

  AuraChatModel(ModelOptions options, WebTarget target) {
    this.listeners = options.listeners();
    this.target = target;
    this.defaultParameters = ChatRequestParameters.builder()
        .temperature(ModelOptionUtils.resolveTemperature(options.temperature()))
        .maxOutputTokens(ModelOptionUtils.resolveMaxOutputTokens(options.maxOutputTokens()))
        .build();
  }

  @Override
  public List<ChatModelListener> listeners() {
    return listeners;
  }

  @Override
  public Set<Capability> supportedCapabilities() {
    return Set.of();
  }

  @Override
  public ChatResponse doChat(ChatRequest request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("messages", convertMessages(request.messages()));
    payload.put("stream", false);
    payload.put("use_history", false);
    if (request.parameters().temperature() != null) {
      payload.put("temperature", request.parameters().temperature());
    }
    if (request.parameters().maxOutputTokens() != null) {
      payload.put("max_completion_tokens", request.parameters().maxOutputTokens());
    }

    try (Response response = target.request(MediaType.APPLICATION_JSON)
        .header("X-Requested-By", "ivy")
        .post(Entity.json(payload))) {
      if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
        throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
      }
      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        throw new IllegalStateException("Aura inference request failed with HTTP " + response.getStatus());
      }

      Map<String, Object> responseBody = response.readEntity(new GenericType<>() {});
      Object content = responseBody.get("content");
      if (!(content instanceof String text)) {
        throw new IllegalStateException(INVALID_RESPONSE);
      }
      return ChatResponse.builder().aiMessage(AiMessage.aiMessage(text)).build();
    }
  }

  @Override
  public ChatRequestParameters defaultRequestParameters() {
    return defaultParameters;
  }

  private static List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
    List<Map<String, String>> converted = new ArrayList<>();
    for (ChatMessage message : messages) {
      Map<String, String> convertedMessage = new LinkedHashMap<>();
      if (message instanceof SystemMessage systemMessage) {
        convertedMessage.put("role", "system");
        convertedMessage.put("content", systemMessage.text());
      } else if (message instanceof UserMessage userMessage) {
        convertedMessage.put("role", "user");
        convertedMessage.put("content", textContent(userMessage));
      } else if (message instanceof AiMessage aiMessage) {
        if (aiMessage.toolExecutionRequests() != null && !aiMessage.toolExecutionRequests().isEmpty()) {
          throw new IllegalArgumentException("Aura tool calls are not implemented yet.");
        }
        convertedMessage.put("role", "assistant");
        convertedMessage.put("content", aiMessage.text());
      } else {
        throw new IllegalArgumentException("Aura supports only system, user, and assistant text messages.");
      }
      converted.add(convertedMessage);
    }
    return converted;
  }

  private static String textContent(UserMessage message) {
    StringBuilder text = new StringBuilder();
    for (var content : message.contents()) {
      if (!(content instanceof TextContent textContent)) {
        throw new IllegalArgumentException("Aura supports only text user messages.");
      }
      text.append(textContent.text());
    }
    return text.toString();
  }
}
