package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestChatHistoryJsonParser {

  // ── Fixtures ──────────────────────────────────────────────────────────────

  private static AgentConversationEntry entryWithUsage(String tokenUsageJson) {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid("case-1");
    entry.setTaskUuid("task-1");
    entry.setTokenUsageJson(tokenUsageJson);
    return entry;
  }

  private static final String SINGLE_USAGE = """
      [{"totalTokens":150,"inputTokens":100,"outputTokens":50,
        "modelName":"gpt-4o","durationMs":3000}]
      """;

  private static final String MULTI_USAGE = """
      [{"totalTokens":100,"inputTokens":60,"outputTokens":40,"modelName":"gpt-4o","durationMs":2000},
       {"totalTokens":200,"inputTokens":120,"outputTokens":80,"modelName":"gpt-4o","durationMs":4000}]
      """;

  // ── getTotalTokens ─────────────────────────────────────────────────────────

  @Test
  void getTotalTokens_singleRecord_returnsValue() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithUsage(SINGLE_USAGE))).isEqualTo(150);
  }

  @Test
  void getTotalTokens_multipleRecords_returnsSum() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithUsage(MULTI_USAGE))).isEqualTo(300);
  }

  @Test
  void getTotalTokens_nullEntry_returnsZero() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(null)).isEqualTo(0);
  }

  @Test
  void getTotalTokens_nullJson_returnsZero() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithUsage(null))).isEqualTo(0);
  }

  // ── getInputTokens / getOutputTokens ──────────────────────────────────────

  @Test
  void getInputTokens_singleRecord_returnsValue() {
    assertThat(ChatHistoryJsonParser.getInputTokens(entryWithUsage(SINGLE_USAGE))).isEqualTo(100L);
  }

  @Test
  void getOutputTokens_singleRecord_returnsValue() {
    assertThat(ChatHistoryJsonParser.getOutputTokens(entryWithUsage(SINGLE_USAGE))).isEqualTo(50L);
  }

  @Test
  void getInputTokens_multipleRecords_returnsSummedInputs() {
    assertThat(ChatHistoryJsonParser.getInputTokens(entryWithUsage(MULTI_USAGE))).isEqualTo(180L);
  }

  @Test
  void getOutputTokens_multipleRecords_returnsSummedOutputs() {
    assertThat(ChatHistoryJsonParser.getOutputTokens(entryWithUsage(MULTI_USAGE))).isEqualTo(120L);
  }

  @Test
  void getInputTokens_nullEntry_returnsZero() {
    assertThat(ChatHistoryJsonParser.getInputTokens(null)).isEqualTo(0L);
  }

  // ── getModelName ───────────────────────────────────────────────────────────

  @Test
  void getModelName_returnsFirstRecordModel() {
    assertThat(ChatHistoryJsonParser.getModelName(entryWithUsage(SINGLE_USAGE))).isEqualTo("gpt-4o");
  }

  @Test
  void getModelName_emptyArray_returnsUnknown() {
    assertThat(ChatHistoryJsonParser.getModelName(entryWithUsage("[]"))).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  @Test
  void getModelName_nullEntry_returnsUnknown() {
    assertThat(ChatHistoryJsonParser.getModelName(null)).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  // ── getAvgDurationMs ──────────────────────────────────────────────────────

  @Test
  void getAvgDurationMs_multipleRecords_returnsAverage() {
    // (2000 + 4000) / 2 = 3000
    assertThat(ChatHistoryJsonParser.getAvgDurationMs(entryWithUsage(MULTI_USAGE))).isEqualTo(3000L);
  }

  @Test
  void getAvgDurationMs_singleRecord_returnsThatValue() {
    assertThat(ChatHistoryJsonParser.getAvgDurationMs(entryWithUsage(SINGLE_USAGE))).isEqualTo(3000L);
  }

  @Test
  void getAvgDurationMs_nullEntry_returnsZero() {
    assertThat(ChatHistoryJsonParser.getAvgDurationMs(null)).isEqualTo(0L);
  }

  // ── getMessageCount ───────────────────────────────────────────────────────

  @Test
  void getMessageCount_validJson_returnsMsgCount() {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid("case-1");
    entry.setMessagesJson("[{\"type\":\"USER\",\"contents\":[{\"type\":\"TEXT\",\"text\":\"hi\"}]}]");
    assertThat(ChatHistoryJsonParser.getMessageCount(entry)).isEqualTo(1);
  }

  @Test
  void getMessageCount_nullEntry_returnsZero() {
    assertThat(ChatHistoryJsonParser.getMessageCount(null)).isEqualTo(0);
  }
}
