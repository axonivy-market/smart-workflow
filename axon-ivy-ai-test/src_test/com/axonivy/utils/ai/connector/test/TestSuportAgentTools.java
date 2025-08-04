package com.axonivy.utils.ai.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector.OpenAiConf;

import AgentDemo.SupportAgentData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;

@IvyProcessTest(enableWebServer = true)
class TestSuportAgentTools {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("SupportAgentTools");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl());
    fixture.var(OpenAiConf.TEST_HEADER, "tool");
  }

  @Test
  void agentTicketCreation(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentData) res.data().onElement(BpmElement.pid("197F7D477CB93027-f165")).getLast();
    assertThat(ticketDone.getCategorizedResult())
        .isEqualTo("technical");
  }

}
