package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.listener.AbstractChatModelListener;
import com.axonivy.utils.smart.workflow.governance.listener.ChatHistoryRecordingListener;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;

public class ChatHistoryRepository {

  private final ChatHistoryRecordingListener listener;
  private final String caseUuid;
  private final String taskUuid;

  private ChatHistoryEntry currentEntry;

  public ChatHistoryRepository(ChatHistoryRecordingListener listener, String caseUuid, String taskUuid) {
    this.listener = listener;
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
  }

  public void updateMessages(List<ChatMessage> messages) {
    var entry = findOrCreateEntry();
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(LocalDateTime.now());
    captureTokenUsageIfNeeded(entry, messages);
    Ivy.repo().save(entry);
    currentEntry = entry;
  }

  private ChatHistoryEntry findOrCreateEntry() {
    var entry = resolveMemoryEntry();
    if (entry != null) {
      return entry;
    }
    var newEntry = new ChatHistoryEntry();
    newEntry.setCaseUuid(caseUuid);
    newEntry.setTaskUuid(taskUuid);
    return newEntry;
  }

  private ChatHistoryEntry resolveMemoryEntry() {
    if (currentEntry != null) {
      return currentEntry;
    }
    var results = Ivy.repo().search(ChatHistoryEntry.class)
        .execute().getAll().stream()
        .filter(e -> caseUuid.equalsIgnoreCase(e.getCaseUuid()) && taskUuid.equalsIgnoreCase(e.getTaskUuid()))
        .toList();
    if (results.isEmpty()) {
      return null;
    }
    var sorted = results.stream()
        .sorted(Comparator.comparing(ChatHistoryEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    removeDuplicates(sorted);
    currentEntry = sorted.get(0);
    return currentEntry;
  }

  private void removeDuplicates(List<ChatHistoryEntry> sorted) {
    sorted.subList(1, sorted.size()).forEach(Ivy.repo()::delete);
  }

  private void captureTokenUsageIfNeeded(ChatHistoryEntry entry, List<ChatMessage> messages) {
    boolean lastMessageIsAi = !messages.isEmpty() && messages.getLast() instanceof AiMessage;

    if (listener == null || !lastMessageIsAi) {
      return;
    }
    var meta = listener.drainPending();
    if (meta != null) {
      appendTokenMetadata(entry, meta);
    }
  }

  private void appendTokenMetadata(ChatHistoryEntry entry,
      AbstractChatModelListener.ResponseMetadata meta) {
    try {
      List<AbstractChatModelListener.ResponseMetadata> list = StringUtils.isBlank(entry.getTokenUsageJson())
          ? new ArrayList<>()
          : JsonUtils.getObjectMapper().readValue(entry.getTokenUsageJson(),
              new TypeReference<List<AbstractChatModelListener.ResponseMetadata>>() {});
      list.add(meta);
      entry.setTokenUsageJson(JsonUtils.getObjectMapper().writeValueAsString(list));
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to persist token usage metadata", ex);
    }
  }
}
