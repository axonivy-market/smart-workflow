package com.axonivy.utils.smart.workflow.demo.erp.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;
import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.erp.shopping.mock.ShoppingChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestProductAgent {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("shopping"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new ShoppingChat()::productAgentResponse);
  }

  @Test
  void productAgent(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testProductAgent")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getProductAgentResult()).isEqualTo("Product agent processed successfully.");
  }
}
