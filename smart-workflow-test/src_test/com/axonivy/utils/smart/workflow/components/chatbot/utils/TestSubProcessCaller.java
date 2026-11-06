package com.axonivy.utils.smart.workflow.components.chatbot.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.components.chatbot.mock.ChatbotChat;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestSubProcessCaller {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("chatbot-adapter"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    fixture.var(GuardrailCollector.DEFAULT_OUTPUT_GUARDRAILS, "");
    fixture.var("AI.CircuitBreaker.Enabled", "false");
  }

  @Test
  void call_passesParamAndReturnsResults() {
    var ai = ChatbotChat.answering("Hello!");

    var result = SubProcessCaller.call("askChatbotAgent(String,List<String>,String)", Map.of("query", "Hi"));

    assertThat(ai.lastUserMessage()).isEqualTo("Hi");
    assertThat(result).containsEntry("answer", "Hello!");
  }

  @Test
  void call_asSessionUser_findsAndCallsSubProcess() throws Exception {
    ChatbotChat.answering("Hello!");
    Ivy.session().loginSessionUser("James", "secret");
    var session = Ivy.session();

    // the test thread runs as system; a dialog request does not
    var result = session.getSecurityContext().executeAs(
        () -> SubProcessCaller.call("askChatbotAgent(String,List<String>,String)", Map.of("query", "Hi")), session);

    assertThat(result).containsEntry("answer", "Hello!");
  }

  @Test
  void call_unknownSignature_fails() {
    assertThatThrownBy(() -> SubProcessCaller.call("unknownProcess(String)", Map.of("query", "Hi")))
        .hasMessageContaining("Expected one subprocess with signature 'unknownProcess(String)', found 0.");
  }
}
