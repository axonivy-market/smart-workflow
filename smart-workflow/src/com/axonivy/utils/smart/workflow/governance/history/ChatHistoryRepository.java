package com.axonivy.utils.smart.workflow.governance.history;

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
import dev.langchain4j.data.message.ChatMessageSerializer;

public class ChatHistoryRepository implements HistoryRecorder {

  /** In-memory store used in tests where OpenSearch is unavailable. Null in production. */
  static List<ChatHistoryEntry> cachedHistoryEntries = null;

  private final String caseUuid;
  private final String taskUuid;

  private ChatHistoryEntry currentEntry;

  public ChatHistoryRepository(String caseUuid, String taskUuid) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
  }

  @Override
  public void store(List<ChatMessage> messages, ResponseMetadata metadata) {
    var entry = findOrCreateEntry();
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(LocalDateTime.now());
    if (metadata != null && !messages.isEmpty() && messages.getLast() instanceof AiMessage) {
      appendTokenMetadata(entry, metadata);
    }
    save(entry);
    currentEntry = entry;
  }

  private ChatHistoryEntry findOrCreateEntry() {
    var entry = loadAndDeduplicateEntry();
    if (entry != null) {
      return entry;
    }
    var newEntry = new ChatHistoryEntry();
    newEntry.setCaseUuid(caseUuid);
    newEntry.setTaskUuid(taskUuid);
    return newEntry;
  }

  private ChatHistoryEntry loadAndDeduplicateEntry() {
    if (currentEntry != null) {
      return currentEntry;
    }
    var source = cachedHistoryEntries != null ? cachedHistoryEntries : Ivy.repo().search(ChatHistoryEntry.class).execute().getAll();
    var results = source.stream()
        .filter(e -> caseUuid.equalsIgnoreCase(e.getCaseUuid()) && taskUuid.equalsIgnoreCase(e.getTaskUuid()))
        .toList();
    if (results.isEmpty()) {
      return null;
    }
    currentEntry = results.stream()
        .max(Comparator.comparing(ChatHistoryEntry::getLastUpdated, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElseThrow();
    results.stream().filter(e -> e != currentEntry).forEach(this::delete);
    return currentEntry;
  }

  private void save(ChatHistoryEntry entry) {
    if (cachedHistoryEntries != null) {
      if (!cachedHistoryEntries.contains(entry)) cachedHistoryEntries.add(entry);
    } else {
      Ivy.repo().save(entry);
    }
  }

  private void delete(ChatHistoryEntry entry) {
    if (cachedHistoryEntries != null) cachedHistoryEntries.remove(entry);
    else Ivy.repo().delete(entry);
  }

  private void appendTokenMetadata(ChatHistoryEntry entry, ResponseMetadata metadata) {
    try {
      List<ResponseMetadata> list = StringUtils.isBlank(entry.getTokenUsageJson())
          ? new ArrayList<>()
          : JsonUtils.getObjectMapper().readValue(entry.getTokenUsageJson(),
              new TypeReference<List<ResponseMetadata>>() {});
      list.add(metadata);
      entry.setTokenUsageJson(JsonUtils.getObjectMapper().writeValueAsString(list));
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to persist token usage metadata", ex);
    }
  }
}
