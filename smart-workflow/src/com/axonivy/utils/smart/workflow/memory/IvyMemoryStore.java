package com.axonivy.utils.smart.workflow.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class IvyMemoryStore implements ChatMemoryStore {

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    return Optional.ofNullable(resolve(memoryId).messages).orElseGet(ArrayList::new);
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    var msgs = resolve(memoryId);
    msgs.messages = messages;
    Ivy.repo().save(msgs);
  }

  private Messages resolve(Object memoryId) {
    var msgs = Ivy.repo().find((String)memoryId, Messages.class);
    if (msgs == null) {
      msgs = new Messages();
      msgs.id = (String) memoryId;
    }
    return msgs;
  }

  @Override
  public void deleteMessages(Object memoryId) {
    Ivy.repo().deleteById((String)memoryId);
  }

  public static class Messages {
    public String id;
    public List<ChatMessage> messages;
  }
  
}
