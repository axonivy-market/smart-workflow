package com.axonivy.utils.smart.workflow.model.aura.internal;

import java.util.List;
import java.util.Set;

import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

public final class AuraChatModel implements ChatModel {

  private final AuraInferenceClient client;
  private final ChatRequestParameters parameters;
  private final List<ChatModelListener> listeners;
  private final Set<Capability> capabilities;

  public AuraChatModel(AuraInferenceClient client, ModelOptions options) {
    if (options.modelName() != null && !options.modelName().isBlank()) {
      throw new IllegalArgumentException("Aura does not support model selection; leave Model empty");
    }
    if (options.hasTools()) {
      throw new IllegalArgumentException("Aura does not support native tools; leave Available tools empty");
    }
    this.client = client;
    this.parameters = ChatRequestParameters.builder()
        .temperature(options.temperature())
        .maxOutputTokens(options.maxOutputTokens())
        .build();
    this.listeners = List.copyOf(options.listeners());
    this.capabilities = options.structuredOutput() ? Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA) : Set.of();
  }

  @Override
  public ChatResponse doChat(ChatRequest request) {
    var body = AuraInferenceProtocol.request(request);
    return AuraInferenceProtocol.response(client.infer(body));
  }

  @Override
  public ChatRequestParameters defaultRequestParameters() {
    return parameters;
  }

  @Override
  public List<ChatModelListener> listeners() {
    return listeners;
  }

  @Override
  public Set<Capability> supportedCapabilities() {
    return capabilities;
  }
}
