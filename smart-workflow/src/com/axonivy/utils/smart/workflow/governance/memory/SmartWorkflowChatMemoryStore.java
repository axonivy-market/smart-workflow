package com.axonivy.utils.smart.workflow.governance.memory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class SmartWorkflowChatMemoryStore implements ChatMemoryStore {

  private static final String FIELD_MEMORY_ID = "memoryId";

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
    var entry = resolveMemoryEntry(String.valueOf(memoryId));
    if (entry == null || StringUtils.isBlank(entry.getMessagesJson())) {
      return new ArrayList<>();
    }
    return ChatMessageDeserializer.messagesFromJson(entry.getMessagesJson());
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    var entry = findOrCreateEntry(String.valueOf(memoryId));
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(LocalDateTime.now());
    captureTokenUsageIfNeeded(entry, messages);
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

  private ChatMemoryEntry findOrCreateEntry(String id) {
    var entry = resolveMemoryEntry(id);
    if (entry != null) {
      return entry;
    }
    var newEntry = new ChatMemoryEntry();
    newEntry.setMemoryId(id);
    newEntry.setAgentId(agentId);
    newEntry.setCaseUuid(caseUuid);
    return newEntry;
  }

  private ChatMemoryEntry resolveMemoryEntry(String memoryId) {
    if (memoryId == null) {
      return null;
    }

    if (cachedEntry != null && memoryId.equals(cachedEntry.getMemoryId())) {
      return cachedEntry;
    }

    var results = Ivy.repo().search(ChatMemoryEntry.class)
        .textField(FIELD_MEMORY_ID)
        .isEqualToIgnoringCase(memoryId)
        .execute()
        .getAll();
    if (results.isEmpty()) {
      return null;
    }
    var sorted = results.stream()
        .sorted(Comparator.comparing(ChatMemoryEntry::getLastUpdated,
            Comparator.nullsFirst(Comparator.reverseOrder())))
        .toList();
    removeDuplicates(sorted);
    cachedEntry = sorted.get(0);
    return cachedEntry;
  }

  private void removeDuplicates(List<ChatMemoryEntry> sorted) {
    sorted.subList(1, sorted.size()).forEach(e -> Ivy.repo().delete(e));
  }

  private void captureTokenUsageIfNeeded(ChatMemoryEntry entry, List<ChatMessage> messages) {
    boolean lastMessageIsAi = !messages.isEmpty() && messages.getLast() instanceof AiMessage;

    if (listener == null || !lastMessageIsAi) {
      return;
    }
    var meta = listener.drainPending();
    if (meta != null) {
      appendTokenMetadata(entry, meta);
    }
  }

  private void appendTokenMetadata(ChatMemoryEntry entry,
      SmartWorkflowChatModelListener.ResponseMetadata meta) {
    try {
      List<SmartWorkflowChatModelListener.ResponseMetadata> list = StringUtils.isBlank(entry.getTokenUsageJson())
          ? new ArrayList<>()
          : JsonUtils.getObjectMapper().readValue(entry.getTokenUsageJson(),
              new TypeReference<List<SmartWorkflowChatModelListener.ResponseMetadata>>() {});
      list.add(meta);
      entry.setTokenUsageJson(JsonUtils.getObjectMapper().writeValueAsString(list));
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to persist token usage metadata", ex);
    }
  }
}
