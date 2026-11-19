package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestPromptBuilderService {

  @Test
  void build_formatsConversationMessagesInPrompt() {
    var entry = entry("[{\"type\":\"user\",\"text\":\"hello\"},{\"type\":\"ai\",\"text\":\"world\"}]");

    var prompt = PromptBuilderService.build("case-1", "Case", List.of(), List.of(entry));

    assertThat(prompt).contains("[USER] hello", "[AI] world");
  }

  @Test
  void build_extractsTextSegmentsAndIgnoresOtherContentTypes() {
    var entry = entry("[{\"type\":\"ai\",\"contents\":[{\"type\":\"TEXT\",\"text\":\"kept\"},"
        + "{\"type\":\"IMAGE\",\"text\":\"ignored\"}]}]");

    var prompt = PromptBuilderService.build("case-1", "Case", List.of(), List.of(entry));

    assertThat(prompt).contains("[AI] kept").doesNotContain("ignored");
  }

  @Test
  void build_invalidConversationJsonMarksDataUnavailable() {
    var entry = entry("not-json{{");

    var prompt = PromptBuilderService.build("case-1", "Case", List.of(), List.of(entry));

    assertThat(prompt).contains("(conversation data unavailable)");
  }

  private static AgentConversationEntry entry(String messagesJson) {
    var entry = new AgentConversationEntry();
    entry.setAgentId("agent");
    entry.setMessagesJson(messagesJson);
    return entry;
  }
}
