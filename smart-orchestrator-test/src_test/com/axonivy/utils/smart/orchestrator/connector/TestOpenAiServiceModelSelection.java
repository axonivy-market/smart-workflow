package com.axonivy.utils.smart.orchestrator.connector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.log.ResourceResponse;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.model.openai.OpenAiChatModelName;

@IvyProcessTest(enableWebServer = true)
class TestOpenAiServiceModelSelection {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());
  @RegisterExtension
  ResourceResponse responder = new ResourceResponse();

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("modelName"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(this::modelInfo);
  }

  private Response modelInfo(JsonNode request) {
    String modelName = request.get("model").asText();
    String expect = OpenAiChatModelName.GPT_3_5_TURBO_1106.toString();
    if (Objects.equals(modelName, expect)) {
      return responder.send("completions-response.json");
    }
    return Response.serverError()
        .entity("this is not the selected model from the AgentCall ui: expected " + expect + " but was " + modelName)
        .build();
  }

  @Test
  void customModel(BpmClient client, AppFixture fixture) {
    fixture.var(OpenAiConf.MODEL, OpenAiChatModelName.O3.toString());

    var res = client.start().process(AGENT_TOOLS.elementName("modelName")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getResult()).isNotBlank();
  }

}
