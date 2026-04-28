package com.axonivy.utils.smart.workflow.memory.store;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class BusinessDataMemory implements ChatMemoryStore {

  @Override
  public void deleteMessages(Object id) {
    Ivy.repo().deleteById((String)id);
  }

  @Override
  public List<ChatMessage> getMessages(Object id) {
    return findMemory(id).map(m -> m.getMessages()).orElse(List.of());
  }

  private Optional<ChatMemory> findMemory(Object id) {
    return Optional.ofNullable(Ivy.repo().find((String)id, ChatMemory.class));
  }

  @Override
  public void updateMessages(Object id, List<ChatMessage> messages) {
    var existing = findMemory(id);
    if (existing.isEmpty()) {
      var memory = new ChatMemory((String)id, messages);
      Ivy.repo().save(memory);
    } else {
      var memory = existing.get();
      memory = new ChatMemory(memory.id, messages);
      Ivy.repo().save(memory);
    }
  }

  public static record ChatMemory(String id, String messages) {

    public ChatMemory(String id, List<ChatMessage> messages) {
      this(id, read(messages));
    }

    private static String read(List<ChatMessage> messages) {
      return MessageSerializer.write(messages);
    }

    public List<ChatMessage> getMessages() {
      return MessageSerializer.read(messages);
    }

  }

}
