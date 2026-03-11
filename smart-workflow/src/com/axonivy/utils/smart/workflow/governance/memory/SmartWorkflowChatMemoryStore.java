package com.axonivy.utils.smart.workflow.governance.memory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

  private final SmartWorkflowChatModelListener listener;
  private final String caseUuid;
  private final String taskUuid;
  private final Supplier<List<ChatMemoryEntry>> finder;
  private final Consumer<ChatMemoryEntry> saver;
  private final Consumer<ChatMemoryEntry> deleter;

  private ChatMemoryEntry cachedEntry;

  public SmartWorkflowChatMemoryStore(SmartWorkflowChatModelListener listener, String caseUuid, String taskUuid) {
    this(listener, caseUuid, taskUuid,
        () -> Ivy.repo().search(ChatMemoryEntry.class)
            .textField("caseUuid").isEqualToIgnoringCase(caseUuid)
            .and().textField("taskUuid").isEqualToIgnoringCase(taskUuid)
            .execute().getAll(),
        entry -> Ivy.repo().save(entry),
        entry -> Ivy.repo().delete(entry));
  }

  SmartWorkflowChatMemoryStore(SmartWorkflowChatModelListener listener, String caseUuid, String taskUuid,
      Supplier<List<ChatMemoryEntry>> finder, Consumer<ChatMemoryEntry> saver,
      Consumer<ChatMemoryEntry> deleter) {
    this.listener = listener;
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
    this.finder = finder;
    this.saver = saver;
    this.deleter = deleter;
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    var entry = resolveMemoryEntry();
    if (entry == null || StringUtils.isBlank(entry.getMessagesJson())) {
      return new ArrayList<>();
    }
    return ChatMessageDeserializer.messagesFromJson(entry.getMessagesJson());
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    var entry = findOrCreateEntry();
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(LocalDateTime.now());
    captureTokenUsageIfNeeded(entry, messages);
    saver.accept(entry);
    cachedEntry = entry;
  }

  @Override
  public void deleteMessages(Object memoryId) {
    finder.get().forEach(deleter);
    cachedEntry = null;
  }

  private ChatMemoryEntry findOrCreateEntry() {
    var entry = resolveMemoryEntry();
    if (entry != null) {
      return entry;
    }
    var newEntry = new ChatMemoryEntry();
    newEntry.setCaseUuid(caseUuid);
    newEntry.setTaskUuid(taskUuid);
    return newEntry;
  }

  private ChatMemoryEntry resolveMemoryEntry() {
    if (cachedEntry != null) {
      return cachedEntry;
    }
    var results = finder.get();
    if (results.isEmpty()) {
      return null;
    }
    var sorted = results.stream()
        .sorted(Comparator.comparing(ChatMemoryEntry::getLastUpdated,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    removeDuplicates(sorted);
    cachedEntry = sorted.get(0);
    return cachedEntry;
  }

  private void removeDuplicates(List<ChatMemoryEntry> sorted) {
    sorted.subList(1, sorted.size()).forEach(deleter);
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
