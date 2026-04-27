package com.axonivy.utils.smart.workflow.memory.store;

import java.util.List;
import java.util.Optional;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class BusinessDataMemory implements ChatMemoryStore {

  @Override
  public void deleteMessages(Object id) {
    Ivy.repo().deleteById((String)id);
  }

  @Override
  public List<ChatMessage> getMessages(Object id) {
    return findMemory(id).map(m -> m.messages).orElse(List.of());
  }

  private Optional<ChatMemory> findMemory(Object id) {
    return Optional.ofNullable(Ivy.repo().find((String)id, ChatMemory.class));
  }

  @Override
  public void updateMessages(Object id, List<ChatMessage> messages) {
    var existing = findMemory(id);
    if (existing.isEmpty()) {
      var memory = new ChatMemory();
      memory.id = (String)id;
      memory.messages = messages;
      Ivy.repo().save(memory);
    } else {
      var memory = existing.get();
      memory.messages = messages;
      Ivy.repo().save(memory);
    }
  }

  public static class ChatMemory {
    public String id;
    public List<ChatMessage> messages;
  }
  
}
