package com.axonivy.utils.smart.workflow.governance.memory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class SmartWorkflowChatMemoryStore implements ChatMemoryStore {

  private static final String FIELD_MEMORY_ID = "memoryId";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String agentId;
  private final SmartWorkflowChatModelListener listener;
  private final String caseUuid;

  private ChatMemoryEntry cachedEntry;

  public SmartWorkflowChatMemoryStore(String agentId, SmartWorkflowChatModelListener listener, String caseUuid) {
    this.agentId = agentId;
    this.listener = listener;
    this.caseUuid = caseUuid;
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    var entry = findByMemoryId(String.valueOf(memoryId));
    if (entry == null || StringUtils.isBlank(entry.getMessagesJson())) {
      return new ArrayList<>();
    }
    return ChatMessageDeserializer.messagesFromJson(entry.getMessagesJson());
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    String id = String.valueOf(memoryId);
    var entry = findByMemoryId(id);
    if (entry == null) {
      entry = new ChatMemoryEntry();
      entry.setMemoryId(id);
      entry.setAgentId(agentId);
      entry.setCaseUuid(caseUuid);
    }
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(LocalDateTime.now());

    // Capture token usage when the most recent message is an AI response
    if (listener != null && !messages.isEmpty()
        && messages.get(messages.size() - 1) instanceof AiMessage) {
      var meta = listener.drainPending();
      if (meta != null) {
        appendTokenMetadata(entry, meta);
      }
    }

    Ivy.repo().save(entry);
    cachedEntry = entry;
  }

  @Override
  public void deleteMessages(Object memoryId) {
    Ivy.repo().search(ChatMemoryEntry.class)
        .textField(FIELD_MEMORY_ID)
        .isEqualToIgnoringCase(String.valueOf(memoryId))
        .execute()
        .getAll()
        .forEach(e -> Ivy.repo().delete(e));
    cachedEntry = null;
  }

  private ChatMemoryEntry findByMemoryId(String memoryId) {
    if (cachedEntry != null) {
      return cachedEntry;
    }
    var all = Ivy.repo().search(ChatMemoryEntry.class)
        .textField(FIELD_MEMORY_ID)
        .isEqualToIgnoringCase(memoryId)
        .execute()
        .getAll();
    if (all.isEmpty()) {
      return null;
    }
    if (all.size() == 1) {
      cachedEntry = all.get(0);
      return cachedEntry;
    }
    // Duplicates present: keep the most recently updated, delete the rest
    var sorted = all.stream()
        .sorted(Comparator.comparing(ChatMemoryEntry::getLastUpdated,
            Comparator.nullsFirst(Comparator.reverseOrder())))
        .toList();
    sorted.subList(1, sorted.size()).forEach(e -> Ivy.repo().delete(e));
    cachedEntry = sorted.get(0);
    return cachedEntry;
  }

  private void appendTokenMetadata(ChatMemoryEntry entry,
      SmartWorkflowChatModelListener.ResponseMetadata meta) {
    try {
      List<SmartWorkflowChatModelListener.ResponseMetadata> list = new ArrayList<>();
      if (StringUtils.isNotBlank(entry.getTokenUsageJson())) {
        list = MAPPER.readValue(entry.getTokenUsageJson(),
            new TypeReference<List<SmartWorkflowChatModelListener.ResponseMetadata>>() {});
      }
      list.add(meta);
      entry.setTokenUsageJson(MAPPER.writeValueAsString(list));
    } catch (Exception ex) {
      Ivy.log().warn("Failed to persist token usage metadata", ex);
    }
  }
}
