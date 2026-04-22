package com.axonivy.utils.smart.workflow.memory;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.util.GuidUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class IvyMemory implements ChatMemory {

  private final String id;
  private final ChatMemoryStore store;

  public IvyMemory() {
    this(GuidUtil.generateID(), new IvyVolatileStore()) ;
  }

  public IvyMemory(String id, ChatMemoryStore store) {
    this.id = id;
    this.store = store;
  }

  public static IvyMemory of(ICase caze) {
    var memory = caze.customFields().textField("chat.memory.id").getOrNull();
    if (memory == null) {
      memory = GuidUtil.generateID(); // TODO: to be unique: case+element id should be sufficient.
      caze.customFields().textField("chat.memory.id").set(memory);
    }
    return new IvyMemory(memory, new IvyVolatileStore());
  }

  @Override
  public Object id() {
    return id;
  }

  @Override
  public void add(ChatMessage message) {
    var msgs = new ArrayList<ChatMessage>(store.getMessages(id));
   // Ivy.log().error("Adding message to memory with id " + id + ": " + message, new RuntimeException());
    var existing= msgs.stream()
      .filter(m -> Objects.equal(m.type(), message.type()))
      .filter(m -> m.toString().equals(message.toString()))
      .findFirst();
    if (existing.isPresent()) {
      Ivy.log().warn("Message already exists in memory, skipping add: " + message);
      return;
    }

    Ivy.log().info("adding message to memory with id " + id + ": " + message+ " existing messages: " + msgs);

    msgs.add(message);
    store.updateMessages(id, msgs);
  }

  @Override
  public List<ChatMessage> messages() {
    return store.getMessages(id);
  }

  @Override
  public void clear() {
    store.deleteMessages(id);
  }

}
