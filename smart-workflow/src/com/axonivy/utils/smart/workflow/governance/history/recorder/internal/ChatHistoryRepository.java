package com.axonivy.utils.smart.workflow.governance.history.recorder.internal;

import com.axonivy.utils.smart.workflow.utils.DateParsingUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;

public class ChatHistoryRepository implements HistoryRecorder {

  private final String caseUuid;
  private final String taskUuid;
  private final String agentId;
  private final HistoryStorage storage;

  private AgentConversationEntry currentEntry;

  public ChatHistoryRepository(String caseUuid, String taskUuid, String agentId, HistoryStorage storage) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
    this.agentId = agentId;
    this.storage = storage;
  }

  @Override
  public void store(List<ChatMessage> messages, ResponseMetadata metadata) {
    var entry = findOrCreateEntry();
    entry.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
    entry.setLastUpdated(DateParsingUtils.now());
    if (metadata != null && !messages.isEmpty() && messages.getLast() instanceof AiMessage) {
      appendTokenMetadata(entry, metadata);
    }
    storage.save(entry);
    currentEntry = entry;
  }

  private AgentConversationEntry findOrCreateEntry() {
    return loadAndDeduplicateEntry().orElseGet(this::newEntry);
  }

  private AgentConversationEntry newEntry() {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid(caseUuid);
    entry.setTaskUuid(taskUuid);
    entry.setAgentId(agentId);
    return entry;
  }

  private Optional<AgentConversationEntry> loadAndDeduplicateEntry() {
    if (currentEntry != null) {
      return Optional.of(currentEntry);
    }
    var results = storage.findAll().stream()
        .filter(e -> caseUuid.equalsIgnoreCase(e.getCaseUuid())
            && taskUuid.equalsIgnoreCase(e.getTaskUuid())
            && agentId.equalsIgnoreCase(e.getAgentId()))
        .toList();
    if (results.isEmpty()) {
      return Optional.empty();
    }
    currentEntry = results.stream()
        .max(Comparator.comparing(AgentConversationEntry::getLastUpdated, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElseThrow();
    var duplicates = results.stream().filter(entry -> entry != currentEntry).toList();
    if (!duplicates.isEmpty()) {
      Ivy.log().warn(String.format("Deduplicating %d stale AgentConversationEntry records for caseUuid=%s agentId=%s",
          duplicates.size(), caseUuid, agentId));
      duplicates.forEach(storage::delete);
    }
    return Optional.of(currentEntry);
  }

  private void appendTokenMetadata(AgentConversationEntry entry, ResponseMetadata metadata) {
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
