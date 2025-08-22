package com.axonivy.utils.ai.tools.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.ai.tools.test.support.SupportToolChat;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;

import AgentDemo.SupportAgentToolsData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;

@IvyProcessTest(enableWebServer = true)
class TestSupportAgentTools {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("SupportAgentTools");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(SupportToolChat::toolTest);
  }

  @Test
  void agentTicketCreation(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsData) res.data().onElement(BpmElement.pid("19856884121ED111-f1"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");
  }

}
