package com.axonivy.utils.smart.workflow.governance.history.internal;

import java.io.IOException;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;

public class ChatHistoryJsonParser {

  private static final String FIELD_TOTAL_TOKENS  = "totalTokens";
  private static final String FIELD_INPUT_TOKENS  = "inputTokens";
  private static final String FIELD_OUTPUT_TOKENS = "outputTokens";
  private static final String FIELD_MODEL_NAME    = "modelName";
  private static final String FIELD_DURATION_MS   = "durationMs";
  public static final String  UNKNOWN_MODEL       = "unknown";

  private static final String PARSE_FAILURE_MESSAGE = "ChatHistoryJsonParser: failed to parse %s for caseUuid=%s: %s";

  public record TokenUsage(long totalTokens, String modelName,
      long inputTokens, long outputTokens, long avgDurationMs) {
    public static final TokenUsage EMPTY = new TokenUsage(0L, UNKNOWN_MODEL, 0L, 0L, 0L);
  }

  private ChatHistoryJsonParser() {}

  public static int getMessageCount(AgentConversationEntry entry) {
    if (entry == null || entry.getMessagesJson() == null) return -1;
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getMessagesJson());
      return array.isArray() ? array.size() : -1;
    } catch (IOException e) {
      Ivy.log().warn(String.format(PARSE_FAILURE_MESSAGE,
        "messagesJson", entry.getCaseUuid(), e.getMessage()));
      return -1;
    }
  }

  public static TokenUsage parseTokenUsage(AgentConversationEntry entry) {
    if (entry == null || entry.getTokenUsageJson() == null) {
      return TokenUsage.EMPTY;
    }
    try {
      JsonNode array = JsonUtils.getObjectMapper().readTree(entry.getTokenUsageJson());
      return array.isArray() ? aggregateTokenUsage(array) : TokenUsage.EMPTY;
    } catch (IOException e) {
      Ivy.log().warn(String.format(PARSE_FAILURE_MESSAGE,
        "tokenUsageJson", entry.getCaseUuid(), e.getMessage()));
      return TokenUsage.EMPTY;
    }
  }

  private static TokenUsage aggregateTokenUsage(JsonNode array) {
    long totalTokens = 0L, inputTokens = 0L, outputTokens = 0L;
    long durationSum = 0L;
    int durationCount = 0;
    for (JsonNode node : array) {
      totalTokens  += longValue(node, FIELD_TOTAL_TOKENS);
      inputTokens  += longValue(node, FIELD_INPUT_TOKENS);
      outputTokens += longValue(node, FIELD_OUTPUT_TOKENS);
      JsonNode dur = node.get(FIELD_DURATION_MS);
      if (dur != null && dur.isNumber()) {
        durationSum += dur.longValue();
        durationCount++;
      }
    }
    long avgDurationMs = durationCount == 0 ? 0L : durationSum / durationCount;
    return new TokenUsage(totalTokens, extractModelName(array), inputTokens, outputTokens, avgDurationMs);
  }

  private static String extractModelName(JsonNode array) {
    if (array.isEmpty()) return UNKNOWN_MODEL;
    JsonNode modelNode = array.get(0).get(FIELD_MODEL_NAME);
    return (modelNode != null && !modelNode.isNull()) ? modelNode.asText() : UNKNOWN_MODEL;
  }

  public static long getTotalTokens(AgentConversationEntry entry) {
    return entry == null ? 0L : parseTokenUsage(entry).totalTokens();
  }

  public static String getModelName(AgentConversationEntry entry) {
    return entry == null ? UNKNOWN_MODEL : parseTokenUsage(entry).modelName();
  }

  public static long getAvgDurationMs(AgentConversationEntry entry) {
    return entry == null ? 0L : parseTokenUsage(entry).avgDurationMs();
  }

  private static long longValue(JsonNode node, String field) {
    JsonNode n = node.get(field);
    return (n != null && n.isNumber()) ? n.longValue() : 0L;
  }
}
