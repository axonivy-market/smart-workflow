package com.axonivy.utils.smart.workflow.governance.history.recorder.internal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.recorder.GuardrailExecutionRecorder;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder;
import com.axonivy.utils.smart.workflow.governance.history.recorder.ToolExecutionRecorder;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;

public class ChatHistoryRepository implements HistoryRecorder, ToolExecutionRecorder, GuardrailExecutionRecorder {

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
    entry.setMessagesJson(stripBase64(ChatMessageSerializer.messagesToJson(messages)));
    entry.setLastUpdated(LocalDateTime.now().toString());
    if (metadata != null && !messages.isEmpty() && messages.getLast() instanceof AiMessage) {
      appendTokenMetadata(entry, metadata);
    }
    storage.save(entry);
    currentEntry = entry;
  }

  @Override
  public void record(String toolName, String arguments, String resultText) {
    var entry = findOrCreateEntry();
    String now = LocalDateTime.now().toString();
    var tools = new ArrayList<>(entry.getToolExecutions());
    tools.add(new ToolExecution(toolName, arguments, resultText, now));
    entry.setToolExecutions(tools);
    entry.setLastUpdated(now);
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
        .filter(entry -> caseUuid.equalsIgnoreCase(entry.getCaseUuid())
            && taskUuid.equalsIgnoreCase(entry.getTaskUuid())
            && agentId.equalsIgnoreCase(entry.getAgentId()))
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

  private static String stripBase64(String messagesJson) {
    try {
      var root = JsonUtils.getObjectMapper().readTree(messagesJson);
      root.findParents("base64Data").forEach(node -> {
        if (node instanceof ObjectNode objectNode) {
          objectNode.remove("base64Data");
        }
      });
      return JsonUtils.getObjectMapper().writeValueAsString(root);
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Failed to strip base64 from messages JSON", ex);
      return messagesJson;
    }
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

  
  @Override
  public void recordGuardrail(String guardrailName, String type, String result, String message, String failureMessage, Long durationMs) {
    var entry = findOrCreateEntry();
    String now = LocalDateTime.now().toString();
    var guardrails = new ArrayList<>(entry.getGuardrailExecutions());
    guardrails.add(new GuardrailExecution(guardrailName, type, result, message, failureMessage, durationMs, now));
    entry.setGuardrailExecutions(guardrails);
    entry.setLastUpdated(now);
    storage.save(entry);
    currentEntry = entry;
  }
}
