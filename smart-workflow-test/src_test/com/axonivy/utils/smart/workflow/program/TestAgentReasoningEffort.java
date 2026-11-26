package com.axonivy.utils.smart.workflow.program;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;

@RestResourceTest
class TestAgentReasoningEffort {

  private static final BpmElement REASONING_EFFORT = BpmProcess.name("TestToolUser").elementName("reasoningEffort");
  private static final BpmElement NOT_SET = BpmProcess.name("TestToolUser").elementName("expandQuery");

  private JsonNode request;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("reasoningEffort"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(OpenAiConf.DEFAULT_MODEL, "gpt-5.1");
    MockOpenAI.defineChat(chat -> {
      request = chat;
      return Response.ok(MockOpenAI.json("""
        { "choices": [ { "message": { "role": "assistant", "content": "done" }, "finish_reason": "stop" } ] }
        """)).build();
    });
  }

  @Test
  void reasoningEffort_isSentToModel(BpmClient client) {
    client.start().process(REASONING_EFFORT).execute();
    assertThat(request.get("reasoning_effort").asString()).isEqualTo("low");
  }

  @Test
  void notSet_sendsNone(BpmClient client) {
    client.start().process(NOT_SET).execute();
    assertThat(request.has("reasoning_effort")).isFalse();
  }
}
