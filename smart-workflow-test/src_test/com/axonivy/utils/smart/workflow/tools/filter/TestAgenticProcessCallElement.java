package com.axonivy.utils.smart.workflow.tools.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@RestResourceTest
class TestAgenticProcessCallElement {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tools.filter"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -> toolsFilter(request, responder));
  }

  private Response toolsFilter(JsonNode request, ResourceResponder responder) {
    var tools = (ArrayNode) request.get("tools");
    var toolNames = new ArrayList<String>();
    tools.forEach(tool -> toolNames.add(tool.get("function").get("name").asText()));
    if (toolNames.size() == 1 && "whoami".equals(toolNames.get(0))) {
      return responder.send("response.json");
    }
    return Response.serverError().build();
  }

  @Test
  void toolFilter(BpmClient client) throws NoSuchFieldException {
    var res = client.start().process(AGENT_TOOLS.elementName("reducedTools")).execute();
    TestToolUserData data = res.data().last();
    assertThat((String) data.get("result")).contains("whoami");
  }

}
