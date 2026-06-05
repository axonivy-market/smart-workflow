package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.arize.semconv.trace.SemanticConventions;
import com.arize.semconv.trace.SemanticConventions.LLMProvider;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public class OpenInferenceCollector {

  static final ObjectMapper objectMapper = new ObjectMapper();
  static final ObjectMapper toolParamMapper = new ObjectMapper().setVisibility(
      PropertyAccessor.FIELD,
      Visibility.ANY);

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  private String provider;
  private String model;
  
  private boolean hideInput = false;
  private boolean hideOutput = false;

  public OpenInferenceCollector(String provider, String model) {
    this.provider = toOpenInferenceProvider(provider);
    this.model = model;
  }

  private static String toOpenInferenceProvider(String provider) {
    var lower = provider.toLowerCase();
    if (lower.startsWith("azure")) { // our azure_openai is incompatible
      return SemanticConventions.LLMProvider.AZURE.getValue();
    }
    return Stream.of(SemanticConventions.LLMProvider.values())
        .map(LLMProvider::getValue)
        .filter(v -> Objects.equals(lower, v)).findFirst()
        .orElseGet(() -> lower);
  }

  public OpenInferenceCollector hideInputMessages(boolean hide) {
    this.hideInput = hide;
    return this;
  }

  public OpenInferenceCollector hideOutputMessages(boolean hide) {
    this.hideOutput = hide;
    return this;
  }

  public Map<String, Object> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  public static String textOf(UserMessage msg) {
    return msg.contents().stream()
        .map(c -> c instanceof TextContent tc ? tc.text() : "[" + contentLabel(c) + "]")
        .collect(Collectors.joining(" "));
  }

  private static String contentLabel(Object content) {
    return content.getClass().getSimpleName()
        .replaceAll("(?i)(File)?Content$", "")
        .toLowerCase();
  }

  public void onRequest(ChatRequest request) {
    attributes.putAll(new RequestRecorder(provider, model).handleRequest(request, hideInput));
  }

  public void onResponse(ChatResponse response) {
    attributes.putAll(new ResponseRecorder().handleResponse(response, hideOutput));
  }
}
