package com.axonivy.utils.smart.workflow.tools.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.support.mock.SupportToolChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

@RestResourceTest
class TestIvySubProcessToolExecutor {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new SupportToolChat()::toolTest);
  }

  @Test
  void stringMinimal() throws Exception {
    var queryOnly = ToolExecutionRequest.builder()
        .id("call_kcgCd6eyMWW8pYTl7d6w2IWa")
        .name("createSupportTicket")
        .arguments("{\"query\":\"Computer is beeping after opening AxonIvy Portal\"}")
        .build();

    var result = IvySubProcessToolExecutor.execute(queryOnly);
    JsonNode jsonResult = (ObjectNode) JsonUtils.getObjectMapper().readTree(result.text());
    assertThat(jsonResult.toPrettyString())
        .contains("\"type\" : \"TECHNICAL\"");

    assertThat(jsonResult.properties()).extracting(Entry::getKey)
        .as("serves complex results as JSON")
        .containsOnly("supportTicket", "aiResult");
  }

}
