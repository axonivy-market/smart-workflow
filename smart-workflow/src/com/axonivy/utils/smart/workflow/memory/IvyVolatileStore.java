package com.axonivy.utils.smart.workflow.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * not serious; just easier to test while prototyping
 */
public class IvyVolatileStore implements ChatMemoryStore {
  
  private static final Map<String, List<ChatMessage>> store = new ConcurrentHashMap<>();
  private static final IvyVolatileStore instance = new IvyVolatileStore();

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    return store.computeIfAbsent((String)memoryId, m -> new java.util.ArrayList<>());
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    store.put((String)memoryId, messages);
  }

  @Override
  public void deleteMessages(Object memoryId) {
    store.remove((String)memoryId);
  }

  public static ChatMemoryStore instance() {
    return instance;  
  }
  
}
