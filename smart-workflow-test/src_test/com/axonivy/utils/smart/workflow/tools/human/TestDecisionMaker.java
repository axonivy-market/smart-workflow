package com.axonivy.utils.smart.workflow.tools.human;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.memory.store.BusinessDataMemory;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

@IvyTest
class TestDecisionMaker {

  private static final String TEST_ID = "testId";

  @Test
  void resolve() {
    var ai = AiMessage.builder().toolExecutionRequests(List.of(
        ToolExecutionRequest.builder()
            .name("whoami")
            .id("call_0wz33imZi8wWsSB0Ct72aMl5null")
            .build()))
        .build();
    memorize(List.of(
        new UserMessage("call my tools"),
        ai));

    var decision = new DecisionMaker(TEST_ID);
    decision.resolve("Mr. James Bond");
    List<ChatMessage> recalled = recall();

    assertThat(recalled).hasSize(3);
    var toolResponse = (ToolExecutionResultMessage) recalled.get(2);
    assertThat(toolResponse.text())
        .isEqualTo("Mr. James Bond");
    assertThat(toolResponse.toolName())
        .isEqualTo("whoami");
    assertThat(toolResponse.id())
        .isEqualTo("call_0wz33imZi8wWsSB0Ct72aMl5null");
  }

  @Test
  void resolve_multiTool() {
    var ai = AiMessage.builder().toolExecutionRequests(List.of(
        ToolExecutionRequest.builder()
            .name("whoami")
            .id("call_0wz33imZi8wWsSB0Ct72aMl5null")
            .build(),
        ToolExecutionRequest.builder()
            .name("add")
            .id("call_7hTjTN9eoACwoU35bPrG6CH8")
            .arguments("{\"a\": 2024, \"b\": -1990}")
            .build()))
        .build();
    memorize(List.of(
        new UserMessage("call my tools"),
        ai));

    var decision = new DecisionMaker(TEST_ID);
    decision.resolve("Mr. James Bond");
    decision.resolve("34");
    List<ChatMessage> recalled = recall();

    assertThat(recalled).hasSize(4);
    assertThat(((ToolExecutionResultMessage) recalled.get(2)).text())
        .isEqualTo("Mr. James Bond");
    assertThat(((ToolExecutionResultMessage) recalled.get(3)).text())
        .isEqualTo("34");
  }

  @Test
  void resolve_multiAssist() {
    var ai1 = AiMessage.builder().toolExecutionRequests(List.of(
        ToolExecutionRequest.builder()
            .name("whoami")
            .id("call_0wz33imZi8wWsSB0Ct72aMl5null")
            .build()))
        .build();
    var res1 = ToolExecutionResultMessage.from(ai1.toolExecutionRequests().get(0), "Mr. James Bond");
    var ai2 = AiMessage.builder().toolExecutionRequests(List.of(
        ToolExecutionRequest.builder()
            .name("add")
            .id("call_7hTjTN9eoACwoU35bPrG6CH8")
            .arguments("{\"a\": 2024, \"b\": -1990}")
            .build()))
        .build();
    memorize(List.of(
        new UserMessage("call my tools"),
        ai1,
        res1,
        ai2));

    var decision = new DecisionMaker(TEST_ID);
    decision.resolve("34");
    List<ChatMessage> recalled = recall();

    assertThat(recalled).hasSize(5);
    assertThat(((ToolExecutionResultMessage) recalled.get(4)).text())
        .isEqualTo("34");
  }

  void memorize(List<ChatMessage> msgs) {
    new BusinessDataMemory().updateMessages(TEST_ID, msgs);
  }

  List<ChatMessage> recall() {
    return new BusinessDataMemory().getMessages(TEST_ID);
  }

  @AfterEach
  void cleanup() {
    Ivy.repo().deleteById(TEST_ID);
  }

}
