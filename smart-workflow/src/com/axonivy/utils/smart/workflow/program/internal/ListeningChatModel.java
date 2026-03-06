package com.axonivy.utils.smart.workflow.program.internal;

import java.util.List;
import java.util.Set;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

class ListeningChatModel implements ChatModel {

  private final ChatModel delegate;
  private final List<ChatModelListener> listeners;

  public ListeningChatModel(ChatModel delegate, ChatModelListener... listeners) {
    this.delegate = delegate;
    this.listeners = List.of(listeners);
  }

  @Override
  public ChatResponse doChat(ChatRequest request) {
    return delegate.doChat(request);
  }

  @Override
  public List<ChatModelListener> listeners() {
    return listeners;
  }

  @Override
  public ModelProvider provider() {
    return delegate.provider();
  }

  @Override
  public ChatRequestParameters defaultRequestParameters() {
    return delegate.defaultRequestParameters();
  }

  @Override
  public Set<Capability> supportedCapabilities() {
    return delegate.supportedCapabilities();
  }
}
