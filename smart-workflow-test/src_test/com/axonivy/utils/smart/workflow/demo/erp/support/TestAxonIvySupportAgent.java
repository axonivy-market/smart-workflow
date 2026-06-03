package com.axonivy.utils.smart.workflow.demo.erp.support;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;
import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.erp.support.mock.AxonIvySupportChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestAxonIvySupportAgent {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("support"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new AxonIvySupportChat()::agentResponse);
  }

  @Test
  void askAxonIvySupport(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testAxonIvySupportAgent")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getAgentResult()).isEqualTo("Task has been created successfully for the Portal 404 error on Cockpit page. Please check the support task for details.");
  }
}
