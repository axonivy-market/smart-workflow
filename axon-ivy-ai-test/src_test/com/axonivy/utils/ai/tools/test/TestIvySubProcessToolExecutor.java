package com.axonivy.utils.ai.tools.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.ai.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.ai.tools.test.support.SupportToolChat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

@IvyProcessTest(enableWebServer = true)
class TestIvySubProcessToolExecutor {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(SupportToolChat::toolTest);
  }

  @Test
  void stringMinimal() throws Exception {
    var queryOnly = ToolExecutionRequest.builder()
        .id("call_eyP0Rh5guxfQGTRGuHpGfD5h")
        .name("createSupportTicket")
        .arguments("{\"query\":\"Computer is beeping after opening AxonIvy Portal\"}")
        .build();

    var result = IvySubProcessToolExecutor.execute(queryOnly);
    assertThat(result.text())
        .contains("[TECHNICAL]");

    var jResult = (ObjectNode) new ObjectMapper().reader().readTree(result.text());
    assertThat(jResult.properties()).extracting(Entry::getKey)
        .as("serves complex results as JSON")
        .containsOnly("supportTicket", "aiResult");
  }

}
