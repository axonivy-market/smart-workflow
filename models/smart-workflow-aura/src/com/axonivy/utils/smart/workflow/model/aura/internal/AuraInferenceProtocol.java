package com.axonivy.utils.smart.workflow.model.aura.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.JsonSchemaElementJsonUtils;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class AuraInferenceProtocol {

  private static final String MULTIPLE_SYSTEM_MESSAGES_MESSAGE = "Aura accepts only one system message";
  private static final String SYSTEM_PROMPT_TOO_LONG_MESSAGE = "Aura system instructions must not exceed 10000 characters";
  private static final String UNSUPPORTED_FILE_INPUT_MESSAGE = "Aura supports text input only; file inputs are not supported";
  private static final String UNSUPPORTED_TOOL_MESSAGES_MESSAGE = "Aura does not support tool calls or tool-result messages";
  private static final String BLANK_MESSAGE_CONTENT_MESSAGE = "Aura messages must contain non-blank text";
  private static final String INVALID_MESSAGE_COUNT_MESSAGE = "Aura requires between 1 and 50 non-system messages";
  private static final String MISSING_JSON_SCHEMA_MESSAGE = "Aura JSON output requires a named JSON schema";
  private static final String UNSUPPORTED_MODEL_SELECTION_MESSAGE = "Aura does not support model selection; leave Model empty";
  private static final String UNSUPPORTED_NATIVE_TOOLS_MESSAGE = "Aura does not support native tools; leave Available tools empty";
  private static final String UNSUPPORTED_SAMPLING_OPTIONS_MESSAGE = "Aura does not support topP, topK, penalties or stop sequences";
  private static final String INVALID_TEMPERATURE_MESSAGE = "Aura temperature must be between 0 and 2";
  private static final String INVALID_MAX_OUTPUT_TOKENS_MESSAGE = "Aura max output tokens must be between 1 and 20000";
  private static final String INVALID_RESPONSE_MESSAGE = "Aura returned an invalid inference response";
  private static final String CONTENT_FILTER_BLOCKED_MESSAGE = "Aura blocked the response with its content filter";
  private static final String MISSING_RESPONSE_CONTENT_MESSAGE = "Aura returned an inference response without non-blank content";

  private AuraInferenceProtocol() {
  }

  public static ObjectNode request(ChatRequest request) {
    validateOptions(request);
    var body = JsonUtils.getObjectMapper().createObjectNode();
    var messages = body.putArray("messages");
    for (ChatMessage message : request.messages()) {
      if (message instanceof SystemMessage system) {
        addSystemPrompt(body, system);
      } else if (message instanceof UserMessage user) {
        addTextMessage(messages, "user", userText(user));
      } else if (message instanceof AiMessage ai && !ai.hasToolExecutionRequests()) {
        addTextMessage(messages, "assistant", ai.text());
      } else {
        throw new IllegalArgumentException(UNSUPPORTED_TOOL_MESSAGES_MESSAGE);
      }
    }
    if (messages.isEmpty() || messages.size() > 50) {
      throw new IllegalArgumentException(INVALID_MESSAGE_COUNT_MESSAGE);
    }
    // Ivy owns conversation memory; do not create a second server-side history.
    body.put("use_history", false);
    body.put("stream", false);
    if (request.temperature() != null) {
      body.put("temperature", request.temperature());
    }
    if (request.maxOutputTokens() != null) {
      body.put("max_completion_tokens", request.maxOutputTokens());
    }
    addResponseFormat(body, request.responseFormat());
    return body;
  }

  private static void addResponseFormat(ObjectNode body, ResponseFormat format) {
    if (format == null || format.type() != ResponseFormatType.JSON) {
      return;
    }

    var schema = format.jsonSchema();
    if (schema == null || schema.name() == null || schema.name().isBlank() || schema.rootElement() == null) {
      throw new IllegalArgumentException(MISSING_JSON_SCHEMA_MESSAGE);
    }
    body.set("response_format", JsonUtils.getObjectMapper().valueToTree(Map.of(
        "type", "json_schema",
        "json_schema", Map.of(
            "name", schema.name(),
            "schema", JsonSchemaElementJsonUtils.toMap(schema.rootElement()),
            "strict", false))));
  }

  private static void addSystemPrompt(ObjectNode body, SystemMessage system) {
    if (body.has("system_prompt")) {
      throw new IllegalArgumentException(MULTIPLE_SYSTEM_MESSAGES_MESSAGE);
    }
    if (system.text().length() > 10000) {
      throw new IllegalArgumentException(SYSTEM_PROMPT_TOO_LONG_MESSAGE);
    }
    body.put("system_prompt", system.text());
  }

  private static String userText(UserMessage user) {
    List<String> parts = new ArrayList<>();
    for (var content : user.contents()) {
      if (!(content instanceof TextContent part)) {
        throw new IllegalArgumentException(UNSUPPORTED_FILE_INPUT_MESSAGE);
      }
      parts.add(part.text());
    }
    return String.join("\n", parts);
  }

  private static void addTextMessage(ArrayNode messages, String role, String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException(BLANK_MESSAGE_CONTENT_MESSAGE);
    }
    messages.addObject().put("role", role).put("content", text);
  }

  private static void validateOptions(ChatRequest request) {
    rejectUnsupportedOptions(request);
    validateTemperature(request.temperature());
    validateMaxOutputTokens(request.maxOutputTokens());
  }

  private static void rejectUnsupportedOptions(ChatRequest request) {
    if (request.modelName() != null && !request.modelName().isBlank()) {
      throw new IllegalArgumentException(UNSUPPORTED_MODEL_SELECTION_MESSAGE);
    }
    if ((request.toolSpecifications() != null && !request.toolSpecifications().isEmpty())
        || request.toolChoice() != null) {
      throw new IllegalArgumentException(UNSUPPORTED_NATIVE_TOOLS_MESSAGE);
    }
    if (request.topP() != null || request.topK() != null || request.frequencyPenalty() != null
        || request.presencePenalty() != null
        || (request.stopSequences() != null && !request.stopSequences().isEmpty())) {
      throw new IllegalArgumentException(UNSUPPORTED_SAMPLING_OPTIONS_MESSAGE);
    }
  }

  private static void validateTemperature(Double temperature) {
    if (temperature != null && (!Double.isFinite(temperature) || temperature < 0 || temperature > 2)) {
      throw new IllegalArgumentException(INVALID_TEMPERATURE_MESSAGE);
    }
  }

  private static void validateMaxOutputTokens(Integer maxTokens) {
    if (maxTokens != null && (maxTokens < 1 || maxTokens > 20000)) {
      throw new IllegalArgumentException(INVALID_MAX_OUTPUT_TOKENS_MESSAGE);
    }
  }

  /** Reads a completed inference result, not a queued-job envelope. */
  public static ChatResponse response(JsonNode response) {
    String content = responseContent(response);

    var finishReason = switch (response.path("finish_reason").asString("")) {
      case "stop" -> FinishReason.STOP;
      case "length" -> FinishReason.LENGTH;
      default -> FinishReason.OTHER;
    };
    // tokens_consumed is billed Aura Credits, not standard model token usage.
    return ChatResponse.builder()
        .aiMessage(AiMessage.from(content))
        .metadata(ChatResponseMetadata.builder().finishReason(finishReason).build())
        .build();
  }

  private static String responseContent(JsonNode response) {
    if (response == null || !response.isObject()) {
      throw new IllegalArgumentException(INVALID_RESPONSE_MESSAGE);
    }
    if ("content_filter".equals(response.path("finish_reason").asString(""))) {
      throw new IllegalStateException(CONTENT_FILTER_BLOCKED_MESSAGE);
    }

    var content = response.get("content");
    if (content == null || !content.isString() || content.asString().isBlank()) {
      throw new IllegalArgumentException(MISSING_RESPONSE_CONTENT_MESSAGE);
    }
    return content.asString();
  }
}
