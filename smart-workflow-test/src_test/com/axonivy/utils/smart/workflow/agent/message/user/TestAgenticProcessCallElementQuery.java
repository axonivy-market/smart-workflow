package com.axonivy.utils.smart.workflow.agent.message.user;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@RestResourceTest
class TestAgenticProcessCallElementQuery {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("query"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -> query(request, responder));
  }

  private Response query(JsonNode request, ResourceResponder responder) {
    var userMessage = request.get("messages").get(0).get("content").asText();
    if (userMessage.contains("ivy.session")) {
      throw new IllegalStateException("given 'query' was not expaned, received: " + userMessage);
    }
    return responder.send("response.json");
  }

  @Test
  void expandQuery(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS.elementName("expandQuery")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getResult()).contains("Unknown User");
  }

}
