package com.axonivy.utils.smart.workflow.model.output;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailProvider;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
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
public class TestMultiModelsOutput {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("output"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(AiConf.DEFAULT_PROVIDER, "");
    fixture.var(OpenAiConf.DEFAULT_MODEL, "");
    fixture.var(GuardrailProvider.DEFAULT_INPUT_GUARDRAILS, "");

    DummyChatModelProvider.defineChatText(r -> "The Spark of Innovation");
  }

  @Test
  void structuredOutput(BpmClient client, ResourceResponder responder) {
    MockOpenAI.defineChat(request -> multiModel(request, responder));

    var res = client.start().process(AGENT_TOOLS.elementName("createStory")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getStory().getTitle())
        .as("got title from first model: DummyProvider")
        .isEqualTo("The Spark of Innovation");
    assertThat(data.getStory().getCharacters())
        .as("got characters from second model: OpenAI")
        .extracting(Person::getFirstName).containsOnly("Reto", "Bruno");
    assertThat(data.getStory().getGenre())
        .as("got genre from default model: OpenAI")
        .isEqualTo("Science Fiction");
  }

  private Response multiModel(JsonNode request, ResourceResponder responder) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() == 2) {
      if ("Only generate the genre field".equals(messages.get(0).get("content").asText())) {
        return responder.send("response2.json");
      }
      return responder.send("response1.json");
    }
    return Response.serverError().build();
  }

}
