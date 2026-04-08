package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.arize.semconv.trace.SemanticConventions;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

class RequestRecorder {
  private final String provider;
  private final String model;
  private final Map<String, Object> attributes = new LinkedHashMap<>();

  public RequestRecorder(String provider, String model) {
    this.provider = provider;
    this.model = model;
  }

  Map<String, Object> handleRequest(ChatRequest request, boolean hideInput) {
    basicAttributes(request);
    ivyAttributes();
    invocationParams(request);
    attributes.putAll(ToolRecorder.toolSpecs(request.toolSpecifications()));
    if (!hideInput) {
      inputMessages(request);
    }
    return attributes;
  }

  private void basicAttributes(ChatRequest request) {
    String modelName = Optional.ofNullable(request.modelName()).orElse(model);

    attributes.put(SemanticConventions.OPENINFERENCE_SPAN_KIND,
        SemanticConventions.OpenInferenceSpanKind.LLM.getValue());
    attributes.put(SemanticConventions.LLM_MODEL_NAME, modelName);
    attributes.put(SemanticConventions.LLM_SYSTEM, "langchain4j");

    attributes.put(SemanticConventions.LLM_PROVIDER, provider);
  }

  private void ivyAttributes() {
    var task = Ivy.wfTask();
    attributes.put("ivy.case", task.getCase().uuid());
    attributes.put("ivy.task", task.uuid());
  }

  private void invocationParams(ChatRequest request) {
    Map<String, Object> invocationParams = new HashMap<>();
    if (request.temperature() != null) {
      invocationParams.put("temperature", request.temperature());
    }
    if (request.maxOutputTokens() != null) {
      invocationParams.put("max_tokens", request.maxOutputTokens());
    }
    if (request.topP() != null) {
      invocationParams.put("top_p", request.topP());
    }
    try {
      attributes.put(SemanticConventions.LLM_INVOCATION_PARAMETERS,
          OpenInferenceCollector.objectMapper.writeValueAsString(invocationParams));
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to serialize invocation parameters", ex);
    }
  }

  private void inputMessages(ChatRequest request) {
    try {
      setInputMessageAttributes(request.messages());
      List<Map<String, Object>> messagesList = convertMessages(request.messages());
      String messagesJson = OpenInferenceCollector.objectMapper.writeValueAsString(messagesList);
      attributes.put(SemanticConventions.INPUT_VALUE, messagesJson);
      attributes.put(SemanticConventions.INPUT_MIME_TYPE, "application/json");
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to serialize input messages", ex);
    }
  }

  private void setInputMessageAttributes(List<ChatMessage> messages) {
    for (int i = 0; i < messages.size(); i++) {
      ChatMessage message = messages.get(i);
      String prefix = String.format("%s.%d.", SemanticConventions.LLM_INPUT_MESSAGES, i);

      attributes.put(prefix + SemanticConventions.MESSAGE_ROLE, mapMessageRole(message.type()));
      attributes.put(prefix + SemanticConventions.MESSAGE_CONTENT, message.toString());

      attributes.putAll(ToolRecorder.recordToolMessages(prefix, message));
    }
  }

  private static String mapMessageRole(ChatMessageType type) {
    return switch (type) {
      case SYSTEM -> "system";
      case USER -> "user";
      case AI -> "assistant";
      case TOOL_EXECUTION_RESULT -> "tool";
      default -> type.toString().toLowerCase();
    };
  }

  private static List<Map<String, Object>> convertMessages(List<ChatMessage> messages) {
    List<Map<String, Object>> result = new ArrayList<>();

    for (ChatMessage message : messages) {
      Map<String, Object> messageMap = new LinkedHashMap<>();

      switch (message.type()) {
        case SYSTEM -> {
          messageMap.put("role", "system");
          if (message instanceof SystemMessage systemMessage) {
            messageMap.put("content", systemMessage.text());
          }
        }
        case USER -> {
          messageMap.put("role", "user");
          if (message instanceof UserMessage userMessage) {
            messageMap.put("content", userMessage.singleText());
          }
        }
        case CUSTOM -> {
          messageMap.put("role", "custom");
          messageMap.put("content", message.toString());
        }
        case AI -> {
          messageMap.put("role", "assistant");
          if (message instanceof AiMessage aiMessage) {
            messageMap.put("content", aiMessage.text());
            if (aiMessage.toolExecutionRequests() != null
                && !aiMessage.toolExecutionRequests().isEmpty()) {
              messageMap.put(
                  "content",
                  Map.of(
                      "tool_calls",
                      aiMessage.toolExecutionRequests().stream()
                          .map(t -> Map.of(
                              "id",
                              t.id(),
                              "function",
                              Map.of("arguments", t.arguments(), "name", t.name())))
                          .collect(Collectors.toList())));
            }
          }
        }
        case TOOL_EXECUTION_RESULT -> {
          messageMap.put("role", "tool");
          if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            messageMap.put("content", toolExecutionResultMessage.text());
            messageMap.put(SemanticConventions.MESSAGE_TOOL_CALL_ID, toolExecutionResultMessage.id());
          }
        }
      }
      result.add(messageMap);
    }

    return result;
  }
}
