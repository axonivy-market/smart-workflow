package com.axonivy.utils.smart.workflow.output;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.test.Person;
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
class TestAgenticProcessCallElementOutput {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("output"));
    fixture.var(OpenAiConf.API_KEY, "");
  }

  @Test
  void structuredOutput(BpmClient client, ResourceResponder responder) {
    MockOpenAI.defineChat(request -> structure(request, responder));

    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutput")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getPerson().getFirstName())
        .isEqualTo("James");
    assertThat(data.getPerson().getLastName())
        .isEqualTo("Bond");
  }

  private Response structure(JsonNode request, ResourceResponder responder) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() == 1) { // tool response
      return responder.send("response1.json");
    }
    if (messages.size() == 3) { // final response
      return responder.send("response2.json");
    }
    return Response.serverError().build();
  }

  @Test
  void structuredOutputList(BpmClient client, ResourceResponder responder) {
    MockOpenAI.defineChat(request -> responder.send("storyResponse.json"));

    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutputList")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getStory().getCharacters())
        .extracting(Person::getFirstName)
        .containsOnly("Reto", "Bruno");
  }

}
