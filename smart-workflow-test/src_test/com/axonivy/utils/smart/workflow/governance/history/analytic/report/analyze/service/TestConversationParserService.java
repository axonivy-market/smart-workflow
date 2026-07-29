package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestConversationParserService {

  @Test
  void parse_simpleTextMessage_returnsFormattedLine() {
    var result = ConversationParserService.parse(entry("[{\"type\":\"user\",\"text\":\"hello world\"}]"));
    assertThat(result).isPresent();
    assertThat(result.get()).contains("[USER]").contains("hello world");
  }

  @Test
  void parse_rootObjectWithMessages_parsedViaMessagesPath() {
    var result = ConversationParserService.parse(entry("{\"messages\":[{\"type\":\"ai\",\"text\":\"hi there\"}]}"));
    assertThat(result).isPresent();
    assertThat(result.get()).contains("[AI]").contains("hi there");
  }

  @Test
  void parse_multipleMessages_allIncluded() {
    var json = "[{\"type\":\"user\",\"text\":\"hello\"},{\"type\":\"ai\",\"text\":\"world\"}]";
    var result = ConversationParserService.parse(entry(json));
    assertThat(result).isPresent();
    assertThat(result.get()).contains("[USER]").contains("hello").contains("[AI]").contains("world");
  }

  @Test
  void parse_contentsArrayWithTextType_extractsText() {
    var json = "[{\"type\":\"user\",\"contents\":[{\"type\":\"TEXT\",\"text\":\"extracted\"}]}]";
    var result = ConversationParserService.parse(entry(json));
    assertThat(result).isPresent();
    assertThat(result.get()).contains("extracted");
  }

  @Test
  void parse_contentsArrayWithNonTextType_ignored() {
    var json = "[{\"type\":\"user\",\"contents\":[{\"type\":\"IMAGE\",\"text\":\"ignored\"}]}]";
    var result = ConversationParserService.parse(entry(json));
    assertThat(result).isPresent();
    assertThat(result.get()).doesNotContain("ignored");
  }

  @Test
  void parse_missingMessageType_usesUnknown() {
    var result = ConversationParserService.parse(entry("[{\"text\":\"no type here\"}]"));
    assertThat(result).isPresent();
    assertThat(result.get()).contains("[UNKNOWN]").contains("no type here");
  }

  @Test
  void parse_blankTextMessage_excluded() {
    var result = ConversationParserService.parse(entry("[{\"type\":\"user\",\"text\":\"   \"}]"));
    assertThat(result).isPresent();
    assertThat(result.get()).isEmpty();
  }

  @Test
  void parse_emptyMessagesArray_returnsEmptyString() {
    var result = ConversationParserService.parse(entry("[]"));
    assertThat(result).isPresent();
    assertThat(result.get()).isEmpty();
  }

  @Test
  void parse_invalidJson_returnsEmpty() {
    var result = ConversationParserService.parse(entry("not-json{{"));
    assertThat(result).isEmpty();
  }

  private static AgentConversationEntry entry(String messagesJson) {
    var entry = new AgentConversationEntry();
    entry.setMessagesJson(messagesJson);
    return entry;
  }
}
