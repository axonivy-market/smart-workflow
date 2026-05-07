package com.axonivy.utils.smart.workflow.memory;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class IvyMemory implements ChatMemory {

  private final String id;
  private final ChatMemoryStore store;

  public IvyMemory(String id, ChatMemoryStore store) {
    this.id = id;
    this.store = store;
  }

  @Override
  public Object id() {
    return id;
  }

  @Override
  public void add(ChatMessage message) {
    var msgs = new ArrayList<ChatMessage>(store.getMessages(id));
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

  @Override
  public String toString() {
    return "IvyMemory{id='" + id + "', messages=" + messages() + "}";
  }

}
