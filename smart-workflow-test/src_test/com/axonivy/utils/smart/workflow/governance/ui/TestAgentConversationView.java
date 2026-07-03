package com.axonivy.utils.smart.workflow.governance.ui;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.ui.entity.AgentConversationView;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentConversationView {

  @Test
  void getLastUpdatedText_null_returnsDash() {
    assertThat(view(new AgentConversationEntry()).getLastUpdatedText()).isEqualTo("—");
  }

  @Test
  void getLastUpdatedText_validDateTime_returnsFormattedString() {
    var entry = new AgentConversationEntry();
    entry.setLastUpdated("2025-06-15T14:30:00");
    var text = view(entry).getLastUpdatedText();
    assertThat(text)
        .isNotNull()
        .isNotEqualTo("—")
        .isNotEqualTo("2025-06-15T14:30:00")
        .contains("2025");
  }

  @Test
  void getLastUpdatedText_invalidFormat_returnsRawValue() {
    var entry = new AgentConversationEntry();
    entry.setLastUpdated("not-a-date");
    assertThat(view(entry).getLastUpdatedText()).isEqualTo("not-a-date");
  }

  @Test
  void getMessageCount_withValidJson_returnsCount() {
    var entry = new AgentConversationEntry();
    entry.setMessagesJson("[{\"type\":\"USER\"},{\"type\":\"AI\"}]");
    assertThat(view(entry).getMessageCount()).isEqualTo(2);
  }

  @Test
  void nullMessagesJson_getMessageCount_returnsInvalid() {
    assertThat(view(new AgentConversationEntry()).getMessageCount()).isEqualTo(-1);
  }

  @Test
  void getTotalTokens_withValidJson_returnsSum() {
    var entry = new AgentConversationEntry();
    entry.setTokenUsageJson("[{\"totalTokens\":20,\"modelName\":\"m\"},{\"totalTokens\":30,\"modelName\":\"m\"}]");
    assertThat(view(entry).getTotalTokens()).isEqualTo(50);
  }

  @Test
  void getTotalTokens_withNullJson_returnsZero() {
    assertThat(view(new AgentConversationEntry()).getTotalTokens()).isEqualTo(0);
  }

  @Test
  void getModelName_withValidJson_returnsFirstModelName() {
    var entry = new AgentConversationEntry();
    entry.setTokenUsageJson("[{\"modelName\":\"gpt-4o\",\"totalTokens\":10}]");
    assertThat(view(entry).getModelName()).isEqualTo("gpt-4o");
  }

  @Test
  void getModelName_withNullJson_returnsUnknown() {
    assertThat(view(new AgentConversationEntry()).getModelName()).isEqualTo(ChatHistoryJsonParser.UNKNOWN_MODEL);
  }

  private static AgentConversationView view(AgentConversationEntry entry) {
    return new AgentConversationView(entry);
  }
}
