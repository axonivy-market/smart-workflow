package com.axonivy.utils.smart.orchestrator.demo.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.demo.support.mock.SupportToolChat;

import AgentDemo.SupportAgentToolsData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.resource.ResourceResponse;

@IvyProcessTest(enableWebServer = true)
@ExtendWith(ResourceResponse.class)
class TestSupportAgentTools {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("SupportAgentTools");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new SupportToolChat()::toolTest);
  }

  @Test
  void agentTicketCreation(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsData) res.data().onElement(BpmElement.pid("19856884121ED111-f1"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");
  }

}
