package com.axonivy.utils.ai.tools.test.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyProcessTest(enableWebServer = true)
class TestAgenticProcessCallElementQuery {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("query"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(this::query);
  }

  private Response query(JsonNode request) {
    var userMessage = request.get("messages").get(0).get("content").asText();
    if (userMessage.contains("ivy.session")) {
      throw new IllegalStateException("given 'query' was not expaned, received: " + userMessage);
    }
    return sendResource("response.json");
  }

  private static Response sendResource(String resource) {
    try (InputStream is = TestAgenticProcessCallElementQuery.class.getResourceAsStream(resource)) {
      return Response.ok()
          .entity(new String(is.readAllBytes()))
          .build();
    } catch (IOException ex) {
      throw new RuntimeException("Failed to load " + resource, ex);
    }
  }

  @Test
  void expandQuery(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS.elementName("expandQuery")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getResult()).contains("Unknown User");
  }

}
