package com.axonivy.utils.smart.workflow.governance.utils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatHistoryJsonParser {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ChatHistoryJsonParser() {}

  public static int getMessageCount(AgentConversationEntry entry) {
    if (entry == null || entry.getMessagesJson() == null) {
      return 0;
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getMessagesJson());
      return array.isArray() ? array.size() : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  public static int getTotalTokens(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0;
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getTokenUsageJson());
      if (!array.isArray()) {
        return 0;
      }
      int total = 0;
      for (JsonNode node : array) {
        JsonNode tokens = node.get("totalTokens");
        if (tokens != null && tokens.isInt()) {
          total += tokens.intValue();
        }
      }
      return total;
    } catch (Exception e) {
      return 0;
    }
  }

  public static String getModelName(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return "unknown";
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getTokenUsageJson());
      if (!array.isArray() || !array.elements().hasNext()) {
        return "unknown";
      }
      JsonNode modelNode = array.get(0).get("modelName");
      return modelNode != null && !modelNode.isNull() ? modelNode.asText() : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }
}
