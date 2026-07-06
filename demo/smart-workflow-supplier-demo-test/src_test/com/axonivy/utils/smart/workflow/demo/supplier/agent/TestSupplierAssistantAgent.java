package com.axonivy.utils.smart.workflow.demo.supplier.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;
import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.supplier.mock.SupplierDemoChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestSupplierAssistantAgent {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("supplier"));
    fixture.var(OpenAiConf.API_KEY, "");
  }

  @Test
  void askSupplierAssistant(BpmClient client) {
    MockOpenAI.defineChat(new SupplierDemoChat()::assistantResponse);
    var res = client.start().process(TEST_PROCESS.elementName("testAskSupplierAssistant")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getAssistantResponse()).isNotBlank();
  }

  @Test
  void parseOnboardingRequest(BpmClient client) {
    MockOpenAI.defineChat(new SupplierDemoChat()::parseResponse);
    var res = client.start().process(TEST_PROCESS.elementName("testParseOnboardingRequest")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getParseFeedback()).isEqualTo("Document parsed successfully.");
  }
}
