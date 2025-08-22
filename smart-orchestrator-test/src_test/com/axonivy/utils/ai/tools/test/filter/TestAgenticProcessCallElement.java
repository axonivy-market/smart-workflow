package com.axonivy.utils.ai.tools.test.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyProcessTest(enableWebServer = true)
class TestAgenticProcessCallElement {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tools.filter"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(this::toolsFilter);
  }

  private Response toolsFilter(JsonNode request) {
    var tools = (ArrayNode) request.get("tools");
    var toolNames = new ArrayList<String>();
    tools.forEach(tool -> toolNames.add(tool.get("function").get("name").asText()));
    if (toolNames.size() == 1 && "whoami".equals(toolNames.get(0))) {
      return sendResource("response.json");
    }
    return Response.serverError().build();
  }

  private static Response sendResource(String resource) {
    try (InputStream is = TestAgenticProcessCallElement.class.getResourceAsStream(resource)) {
      return Response.ok()
          .entity(new String(is.readAllBytes()))
          .build();
    } catch (IOException ex) {
      throw new RuntimeException("Failed to load " + resource, ex);
    }
  }

  @Test
  void toolFilter(BpmClient client) throws NoSuchFieldException {
    var res = client.start().process(AGENT_TOOLS.elementName("reducedTools")).execute();
    TestToolUserData data = res.data().last();
    assertThat((String) data.get("result")).contains("whoami");
  }

}
