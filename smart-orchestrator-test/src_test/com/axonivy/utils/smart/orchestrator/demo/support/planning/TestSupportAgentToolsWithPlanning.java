package com.axonivy.utils.smart.orchestrator.demo.support.planning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;

import AgentDemo.SupportAgentToolsWithPlanningData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;

@IvyProcessTest(enableWebServer = true)
public class TestSupportAgentToolsWithPlanning {
  private static final BpmElement AGENT_TOOLS = BpmProcess.name("SupportAgentToolsWithPlanning")
      .elementName("startPlanning");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(SupportToolWithPlanningChat::toolTest);
  }

  @Test
  void agentTicketCreation(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsWithPlanningData) res.data().onElement(BpmElement.pid("198A1AC925DE5BB4-f6"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");
  }
}
