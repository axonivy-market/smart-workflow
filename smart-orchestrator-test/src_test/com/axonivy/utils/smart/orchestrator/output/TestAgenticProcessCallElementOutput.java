package com.axonivy.utils.smart.orchestrator.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyProcessTest(enableWebServer = true)
class TestAgenticProcessCallElementOutput {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("output"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(this::structure);
  }

  private Response structure(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() == 1) { // tool response
      return sendResource("response1.json");
    }
    if (messages.size() == 3) { // final response
      return sendResource("response2.json");
    }
    return Response.serverError().build();
  }

  private static Response sendResource(String resource) {
    try (InputStream is = TestAgenticProcessCallElementOutput.class.getResourceAsStream(resource)) {
      return Response.ok()
          .entity(new String(is.readAllBytes()))
          .build();
    } catch (IOException ex) {
      throw new RuntimeException("Failed to load " + resource, ex);
    }
  }

  @Test
  void structuredOutput(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutput")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getPerson().getFirstName())
        .isEqualTo("James");
    assertThat(data.getPerson().getLastName())
        .isEqualTo("Bond");
  }

}
