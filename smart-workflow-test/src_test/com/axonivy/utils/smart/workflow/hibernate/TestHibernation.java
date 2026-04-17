package com.axonivy.utils.smart.workflow.hibernate;

import javax.print.DocFlavor.READER;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.tools.java.mock.ToolDemoChat;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;

@RestResourceTest
public class TestHibernation {

  private static final BpmProcess HIBERNATION_PLAYGROUND = BpmProcess.name("Hibernation");
  private final ResourceResponder responder = new ResourceResponder(getClass());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    MockOpenAI.defineChat(new ToolDemoChat()::respond);
  }

  public Response respond(JsonNode request){
    System.out.println(request);
    return Response.ok().build();
  }

  @Test
  void askUserOptions(BpmClient client) {
    MockOpenAI.defineChat(this::respond);
    var result = client.start()
        .process(HIBERNATION_PLAYGROUND.elementName("ask"))
        .execute();

        
    System.out.println(result.data().last());
  }

  @Test
  void askUserOptions_error(BpmClient client) {
    MockOpenAI.defineChat(this::error);
    var result = client.start()
        .process(HIBERNATION_PLAYGROUND.elementName("askError"))
        .execute();

    System.out.println(result.data().last());
  }

  private Response error(JsonNode req) {
    System.out.println(req.toPrettyString());
    return responder.send("humanTool-r1.json");
  }

  @Test
  void askUserYesNo(BpmClient client) {
    var result = client.start()
        .process(HIBERNATION_PLAYGROUND.elementName("askYesOrNo"))
        .execute();

    System.out.println(result.data().last() );
  }
}
