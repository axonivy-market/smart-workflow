package com.axonivy.utils.ai.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

public class AgentChatMemoryProvider implements ChatMemoryProvider {


  public void createNewMemory(Object id) {
    AgentMemoryStore.getInstance().createMemory(id);
  }

  @Override
  public ChatMemory get(Object memoryId) {
    return AgentMemoryStore.getInstance().getMemory(memoryId);
  }

  public void saveMessages(Object memoryId) {
    return;
  }
}
