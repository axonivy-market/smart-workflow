package com.axonivy.utils.smart.workflow.governance.utils;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatHistoryJsonParser {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ChatHistoryJsonParser() {}

  public static int getMessageCount(ChatHistoryEntry entry) {
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

  public static int getTotalTokens(ChatHistoryEntry entry) {
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

  public static String getModelName(ChatHistoryEntry entry) {
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

  public static long getInputTokens(ChatHistoryEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getTokenUsageJson());
      if (!array.isArray()) {
        return 0L;
      }
      long total = 0L;
      for (JsonNode node : array) {
        JsonNode tokens = node.get("inputTokens");
        if (tokens != null && tokens.isNumber()) {
          total += tokens.longValue();
        }
      }
      return total;
    } catch (Exception e) {
      return 0L;
    }
  }

  public static long getOutputTokens(ChatHistoryEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getTokenUsageJson());
      if (!array.isArray()) {
        return 0L;
      }
      long total = 0L;
      for (JsonNode node : array) {
        JsonNode tokens = node.get("outputTokens");
        if (tokens != null && tokens.isNumber()) {
          total += tokens.longValue();
        }
      }
      return total;
    } catch (Exception e) {
      return 0L;
    }
  }

  public static long getAvgDurationMs(ChatHistoryEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = MAPPER.readTree(entry.getTokenUsageJson());
      if (!array.isArray() || array.size() == 0) {
        return 0L;
      }
      long total = 0L;
      int count = 0;
      for (JsonNode node : array) {
        JsonNode duration = node.get("durationMs");
        if (duration != null && duration.isNumber()) {
          total += duration.longValue();
          count++;
        }
      }
      return count == 0 ? 0L : total / count;
    } catch (Exception e) {
      return 0L;
    }
  }
}
