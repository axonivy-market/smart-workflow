package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentConversationEntry {

  // ── getLastUpdatedRaw ────────────────────────────────────────────────────────

  @Test
  void getLastUpdatedRaw_null_returnsEmpty() {
    var entry = new AgentConversationEntry();
    assertThat(entry.getLastUpdatedRaw()).isEqualTo("");
  }

  @Test
  void getLastUpdatedRaw_nonNull_returnsValue() {
    var entry = new AgentConversationEntry();
    entry.setLastUpdated("2025-01-15T10:30:00");
    assertThat(entry.getLastUpdatedRaw()).isEqualTo("2025-01-15T10:30:00");
  }

  // ── getLastUpdatedText ───────────────────────────────────────────────────────

  @Test
  void getLastUpdatedText_null_returnsDash() {
    var entry = new AgentConversationEntry();
    assertThat(entry.getLastUpdatedText()).isEqualTo("—");
  }

  @Test
  void getLastUpdatedText_validDateTime_returnsFormattedString() {
    var entry = new AgentConversationEntry();
    entry.setLastUpdated("2025-06-15T14:30:00");
    var text = entry.getLastUpdatedText();
    assertThat(text)
        .isNotNull()
        .isNotEqualTo("—")
        .isNotEqualTo("2025-06-15T14:30:00") // formatted, not raw ISO
        .contains("2025");
  }

  @Test
  void getLastUpdatedText_invalidFormat_returnsRawValue() {
    var entry = new AgentConversationEntry();
    entry.setLastUpdated("not-a-date");
    assertThat(entry.getLastUpdatedText()).isEqualTo("not-a-date");
  }

  // ── getMessageCount (delegates to ChatHistoryJsonParser) ─────────────────────

  @Test
  void getMessageCount_withValidJson_returnsCount() {
    var entry = new AgentConversationEntry();
    entry.setMessagesJson("[{\"type\":\"USER\"},{\"type\":\"AI\"}]");
    assertThat(entry.getMessageCount()).isEqualTo(2);
  }

  @Test
  void nullMessagesJson_getMessageCount_returnsInvalid() {
    var entry = new AgentConversationEntry();
    assertThat(entry.getMessageCount()).isEqualTo(-1);
  }

  // ── getTotalTokens (delegates to ChatHistoryJsonParser) ──────────────────────

  @Test
  void getTotalTokens_withValidJson_returnsSum() {
    var entry = new AgentConversationEntry();
    entry.setTokenUsageJson("[{\"totalTokens\":20,\"modelName\":\"m\"},{\"totalTokens\":30,\"modelName\":\"m\"}]");
    assertThat(entry.getTotalTokens()).isEqualTo(50);
  }

  @Test
  void getTotalTokens_withNullJson_returnsZero() {
    var entry = new AgentConversationEntry();
    assertThat(entry.getTotalTokens()).isEqualTo(0);
  }

  // ── getModelName (delegates to ChatHistoryJsonParser) ────────────────────────

  @Test
  void getModelName_withValidJson_returnsFirstModelName() {
    var entry = new AgentConversationEntry();
    entry.setTokenUsageJson("[{\"modelName\":\"gpt-4o\",\"totalTokens\":10}]");
    assertThat(entry.getModelName()).isEqualTo("gpt-4o");
  }

  @Test
  void getModelName_withNullJson_returnsUnknown() {
    var entry = new AgentConversationEntry();
    assertThat(entry.getModelName()).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  // ── agentName / processName fields ───────────────────────────────────────────

  @Test
  void agentNameAndProcessName_setAndGetCorrectly() {
    var entry = new AgentConversationEntry();
    entry.setAgentName("My Agent");
    entry.setProcessName("My Process");
    assertThat(entry.getAgentName()).isEqualTo("My Agent");
    assertThat(entry.getProcessName()).isEqualTo("My Process");
  }
}
