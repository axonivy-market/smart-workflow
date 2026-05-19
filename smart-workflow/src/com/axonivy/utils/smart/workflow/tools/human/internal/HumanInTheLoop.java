package com.axonivy.utils.smart.workflow.tools.human.internal;

import java.util.List;
import java.util.UUID;

import com.axonivy.utils.smart.workflow.memory.id.IdStore;
import com.axonivy.utils.smart.workflow.memory.store.BusinessDataMemory;
import com.axonivy.utils.smart.workflow.observability.AiListenerProvider;

import ch.ivyteam.ivy.bpm.error.BpmError;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class HumanInTheLoop implements AiListenerProvider{

  public final IdStore memoryId;
  public final ChatMemoryStore store;

  public HumanInTheLoop(IdStore memoryId, ChatMemoryStore store) {
    this.memoryId = memoryId;
    this.store = store;
  }

  @Override
  public List<AiServiceListener<?>> provide() {
    return List.of(
      new InitListener(), 
      new ErrorListener(), 
      new CompletedListener()
    );
  }

  public List<Content> userMessage(List<Content> userInput) {
    if (memoryId.id().isPresent()) {
      return List.of(TextContent.from("continue with my selection from the tool"));
    }
    return userInput;
  }

  public boolean isRestoredConversion() {
    return memoryId.id().isPresent();
  }

  private class InitListener implements AiServiceStartedListener {
    @Override
    public void onEvent(AiServiceStartedEvent event) {
      memoryId.id().map(new BusinessDataMemory()::getMessages).ifPresent(msgs -> store.updateMessages(
          ChatMemoryService.DEFAULT, msgs)); // inject from human-in-the-loop
    }
  } 

  private class ErrorListener implements AiServiceErrorListener {
    @Override
    public void onEvent(AiServiceErrorEvent event) {
      if (event.error() instanceof BpmError error) {
        if (error.getAttribute("ai.invocationId") instanceof UUID invocationId) {
          memoryId.id(invocationId.toString()); // share with outer consumers!
          List<ChatMessage> messages = store.getMessages(ChatMemoryService.DEFAULT);
          new BusinessDataMemory().updateMessages(invocationId.toString(), messages);
        }
      }
    }
  }

  private class CompletedListener implements AiServiceCompletedListener {
    @Override
    public void onEvent(AiServiceCompletedEvent event) {
      memoryId.id().ifPresent(id -> {
        new BusinessDataMemory().deleteMessages(id);
        memoryId.id("");
      });
    }
  }

}

