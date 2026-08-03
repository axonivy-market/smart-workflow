package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestChatHistoryJsonParser {

  @Test
  void getMessageCount_validJson_returnsArraySize() {
    var entry = entryWithMessages("[{\"type\":\"USER\"},{\"type\":\"AI\"},{\"type\":\"AI\"}]");
    assertThat(ChatHistoryJsonParser.getMessageCount(entry)).isEqualTo(3);
  }

  @Test
  void getMessageCount_emptyArray_returnsZero() {
    assertThat(ChatHistoryJsonParser.getMessageCount(entryWithMessages("[]"))).isEqualTo(0);
  }

  @Test
  void nullInput_getMessageCount_returnsInvalid() {
    assertThat(ChatHistoryJsonParser.getMessageCount(null)).isEqualTo(-1);
    assertThat(ChatHistoryJsonParser.getMessageCount(new AgentConversationEntry())).isEqualTo(-1);
  }

  @Test
  void notAnArray_getMessageCount_returnsInvalid() {
    assertThat(ChatHistoryJsonParser.getMessageCount(entryWithMessages("{\"type\":\"USER\"}"))).isEqualTo(-1);
  }

  @Test
  void invalidJson_getMessageCount_returnsInvalid() {
    assertThat(ChatHistoryJsonParser.getMessageCount(entryWithMessages("NOT_VALID_JSON"))).isEqualTo(-1);
  }

  @Test
  void getTotalTokens_sumsAllEntries() {
    var entry = entryWithTokens("[{\"totalTokens\":10,\"modelName\":\"m\"},{\"totalTokens\":5,\"modelName\":\"m\"}]");
    assertThat(ChatHistoryJsonParser.getTotalTokens(entry)).isEqualTo(15);
  }

  @Test
  void getTotalTokens_emptyArray_returnsZero() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithTokens("[]"))).isEqualTo(0);
  }

  @Test
  void getTotalTokens_returnsZero_forNullInput() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(null)).isEqualTo(0);
    assertThat(ChatHistoryJsonParser.getTotalTokens(new AgentConversationEntry())).isEqualTo(0);
  }

  @Test
  void getTotalTokens_missingTokenField_returnsZero() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithTokens("[{\"modelName\":\"gpt-4\"}]"))).isEqualTo(0);
  }

  @Test
  void getTotalTokens_invalidJson_returnsZero() {
    assertThat(ChatHistoryJsonParser.getTotalTokens(entryWithTokens("NOT_VALID_JSON"))).isEqualTo(0);
  }

  @Test
  void getModelName_returnsFirstEntry() {
    var entry = entryWithTokens("[{\"modelName\":\"gpt-4o\",\"totalTokens\":5},{\"modelName\":\"other\",\"totalTokens\":3}]");
    assertThat(ChatHistoryJsonParser.getModelName(entry)).isEqualTo("gpt-4o");
  }

  @Test
  void getModelName_emptyArray_returnsUnknown() {
    assertThat(ChatHistoryJsonParser.getModelName(entryWithTokens("[]"))).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  @Test
  void getModelName_returnsUnknown_forNullInput() {
    assertThat(ChatHistoryJsonParser.getModelName(null)).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
    assertThat(ChatHistoryJsonParser.getModelName(new AgentConversationEntry())).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  @Test
  void getModelName_returnsUnknown_whenModelNameAbsent() {
    assertThat(ChatHistoryJsonParser.getModelName(entryWithTokens("[{\"modelName\":null,\"totalTokens\":5}]")))
        .isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
    assertThat(ChatHistoryJsonParser.getModelName(entryWithTokens("[{\"totalTokens\":5}]")))
        .isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  @Test
  void getModelName_invalidJson_returnsUnknown() {
    assertThat(ChatHistoryJsonParser.getModelName(entryWithTokens("NOT_VALID_JSON"))).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  private static AgentConversationEntry entryWithMessages(String json) {
    var e = new AgentConversationEntry();
    e.setMessagesJson(json);
    return e;
  }

  private static AgentConversationEntry entryWithTokens(String json) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson(json);
    return e;
  }
}
