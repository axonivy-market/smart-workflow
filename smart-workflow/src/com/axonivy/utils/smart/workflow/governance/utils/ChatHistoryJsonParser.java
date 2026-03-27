package com.axonivy.utils.smart.workflow.governance.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

  public static long getInputTokens(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
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

  public static long getOutputTokens(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
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

  private static final String FIELD_START_TIMESTAMP = "startTimestamp";
  private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss.SSS");

  /** Returns the earliest startTimestamp across all token-usage records as raw ISO string (for sorting). */
  public static String getStartTimestamp(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) return null;
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
      if (!array.isArray()) return null;
      String earliest = null;
      for (JsonNode node : array) {
        JsonNode ts = node.get(FIELD_START_TIMESTAMP);
        if (ts != null && !ts.isNull()) {
          String val = ts.asText();
          if (earliest == null || val.compareTo(earliest) < 0) earliest = val;
        }
      }
      return earliest;
    } catch (Exception e) {
      return null;
    }
  }

  /** Returns the earliest startTimestamp across all token-usage records, formatted for display. */
  public static String getStartTime(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) return "—";
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
      if (!array.isArray()) return "—";
      String earliest = null;
      for (JsonNode node : array) {
        JsonNode ts = node.get(FIELD_START_TIMESTAMP);
        if (ts != null && !ts.isNull()) {
          String val = ts.asText();
          if (earliest == null || val.compareTo(earliest) < 0) earliest = val;
        }
      }
      if (earliest == null) return "—";
      return LocalDateTime.parse(earliest).format(DISPLAY_FMT);
    } catch (Exception e) {
      return "—";
    }
  }

  public static long getAvgDurationMs(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return 0L;
    }
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
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
