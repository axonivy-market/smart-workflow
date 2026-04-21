package com.axonivy.utils.smart.workflow.memory;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.util.GuidUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

public class IvyMemory implements ChatMemory {

  private final String id;

  public IvyMemory() {
    this(GuidUtil.generateID()) ;
  }

  public IvyMemory(String id) {
    this.id = id;
  }

  public static IvyMemory of(ICase caze) {
    var memory = caze.customFields().textField("chat.memory.id").getOrNull();
    if (memory == null) {
      memory = GuidUtil.generateID();
      caze.customFields().textField("chat.memory.id").set(memory);
    }
    return new IvyMemory(memory);
  }

  @Override
  public Object id() {
    return id;
  }

  @Override
  public void add(ChatMessage message) {
    var msgs = new ArrayList<ChatMessage>(new IvyMemoryStore().getMessages(id));
    msgs.add(message);
    new IvyMemoryStore().updateMessages(id, msgs);
  }

  @Override
  public List<ChatMessage> messages() {
    return new IvyMemoryStore().getMessages(id);
  }

  @Override
  public void clear() {
    new IvyMemoryStore().deleteMessages(id);
  }

}
