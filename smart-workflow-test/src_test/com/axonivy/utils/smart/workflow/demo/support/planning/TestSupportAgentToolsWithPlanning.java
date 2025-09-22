package com.axonivy.utils.smart.workflow.demo.support.planning;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector.OpenAiConf;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import AgentDemo.SupportAgentToolsWithPlanningData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;

@RestResourceTest
public class TestSupportAgentToolsWithPlanning {

  private static final BpmElement AGENT_TOOLS = BpmProcess.name("SupportAgentToolsWithPlanning")
      .elementName("startPlanning");

  @Test
  void agentTicketCreation(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsWithPlanningData) res.data().onElement(BpmElement.pid("198A1AC925DE5BB4-f6"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");
  }

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(r -> toolTest(r, responder));
  }

  private Response toolTest(JsonNode request, ResourceResponder responder) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() >= 1) {
      var current = messages.get(messages.size() - 1);

      if ("Help me, my computer is beeping, it started after opening AxonIvy Portal."
          .equals(current.toPrettyString())) {
        return responder.send("response1.json");
      }

      if (messages.size() == 2 && "Instruction to follow:\\n- Don't fill information related to approval\\n[]\\n"
          .equals(messages.get(0).get("content").toPrettyString())) {
        return responder.send("response2.json");
      }

      if ("tool".equals(current.get("role").textValue())
          && current.toPrettyString().contains("\\\"id\\\" : \\\"882bb24b848b4583aca8e7cc503e807d\\\"")) {
        return responder.send("response3.json");
      }

      if ("user".equals(current.get("role").textValue())
          && current.toPrettyString().contains("\\\"id\\\" : \\\"882bb24b848b4583aca8e7cc503e807d\\\"")) {
        return responder.send("response4.json");
      }

      return responder.send("response5.json");
    }
    return Response.status(404).build();
  }
}
