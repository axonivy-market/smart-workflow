package com.axonivy.utils.smart.workflow.governance.utils;

import java.io.IOException;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;

public class ChatHistoryJsonParser {

  private static final String FIELD_TOTAL_TOKENS = "totalTokens";
  private static final String FIELD_MODEL_NAME = "modelName";
  public static final String UNKNOWN_MODEL = "unknown";

  private ChatHistoryJsonParser() {}

  public static int getMessageCount(AgentConversationEntry entry) {
    return Optional.ofNullable(entry)
        .map(AgentConversationEntry::getMessagesJson)
        .map(json -> {
          try {
            JsonNode array = JsonUtils.getObjectMapper().readTree(json);
            return array.isArray() ? array.size() : 0;
          } catch (IOException e) {
            Ivy.log().warn(String.format("ChatHistoryJsonParser: failed to parse messagesJson for caseUuid=%s: %s",
                entry.getCaseUuid(), e.getMessage()));
            return 0;
          }
        })
        .orElse(0);
  }

  public static int getTotalTokens(AgentConversationEntry entry) {
    return Optional.ofNullable(entry)
        .map(AgentConversationEntry::getTokenUsageJson)
        .map(json -> {
          try {
            JsonNode array = JsonUtils.getObjectMapper().readTree(json);
            if (!array.isArray()) {
              return 0;
            }
            int total = 0;
            for (JsonNode node : array) {
              JsonNode tokens = node.get(FIELD_TOTAL_TOKENS);
              if (tokens != null && tokens.isInt()) {
                total += tokens.intValue();
              }
            }
            return total;
          } catch (IOException e) {
            Ivy.log().warn(String.format("ChatHistoryJsonParser: failed to parse tokenUsageJson for caseUuid=%s: %s",
                entry.getCaseUuid(), e.getMessage()));
            return 0;
          }
        })
        .orElse(0);
  }

  public static String getModelName(AgentConversationEntry entry) {
    return Optional.ofNullable(entry)
        .map(AgentConversationEntry::getTokenUsageJson)
        .map(json -> {
          try {
            JsonNode array = JsonUtils.getObjectMapper().readTree(json);
            if (!array.isArray() || !array.elements().hasNext()) {
              return UNKNOWN_MODEL;
            }
            JsonNode modelNode = array.get(0).get(FIELD_MODEL_NAME);
            return modelNode != null && !modelNode.isNull() ? modelNode.asText() : UNKNOWN_MODEL;
          } catch (IOException e) {
            Ivy.log().warn(String.format("ChatHistoryJsonParser: failed to parse tokenUsageJson for caseUuid=%s: %s",
                entry.getCaseUuid(), e.getMessage()));
            return UNKNOWN_MODEL;
          }
        })
        .orElse(UNKNOWN_MODEL);
  }
}
