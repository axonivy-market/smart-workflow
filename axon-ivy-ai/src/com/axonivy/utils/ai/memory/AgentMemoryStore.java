package com.axonivy.utils.ai.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class AgentMemoryStore implements ChatMemoryStore {

  private List<AgentMemory> memories;
  private static AgentMemoryStore instance;

  public static AgentMemoryStore getInstance() {
    if (instance == null) {
      instance = new AgentMemoryStore();
      instance.memories = new ArrayList<>();
    }
    return instance;
  }

  public AgentMemory getMemory(Object memoryId) {
    return findMemoryOptional(memoryId).orElse(null);
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    return findMemoryOptional(memoryId).map(ChatMemory::messages)
        .orElse(new ArrayList<>());
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    if (CollectionUtils.isEmpty(messages)) {
      return;
    }

    Optional<AgentMemory> memoryOpt = findMemoryOptional(memoryId);

    if (memoryOpt.isEmpty()) {
      AgentMemory newMemory = new AgentMemory(memoryId);
      newMemory.add(messages);
      memories.add(newMemory);
    }

    memoryOpt.get().add(messages);
  }

  @Override
  public void deleteMessages(Object memoryId) {
    Optional<AgentMemory> memoryOpt = findMemoryOptional(memoryId);
    if (memoryOpt.isPresent()) {
      memories.remove(memoryOpt.get());
    }
  }

  public void createMemory(Object memoryId) {
    Optional<AgentMemory> memoryOpt = findMemoryOptional(memoryId);
    if (memoryOpt.isPresent()) {
      return;
    }
    memories.add(new AgentMemory(memoryId));
  }

  private Optional<AgentMemory> findMemoryOptional(Object memoryId) {
    return memories.stream().filter(memory -> memory.id().equals(memoryId)).findFirst();
  }

  public void persist(Object memoryId) {
    Optional<AgentMemory> memoryOpt = findMemoryOptional(memoryId);
    if (memoryOpt.isPresent()) {
      AgentMemory memory = memoryOpt.get();
      memory.convertToPersistedMessages();
      Ivy.repo().save(memoryOpt.get());
    }
  }
}
