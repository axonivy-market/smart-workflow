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
class TestSupplierValidationAgent {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("supplier"));
    fixture.var(OpenAiConf.API_KEY, "");
  }

  @Test
  void supplierDocumentExtractAgent(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testSupplierDocumentExtractAgent")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getExtractionResult()).isNotNull();
  }

  @Test
  void validateAgainstPolicy(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testValidateAgainstPolicy")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyValidationResult()).isNotNull();
  }

  @Test
  void callRiskAssessment(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testCallRiskAssessment")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getRiskScoreResult()).isNotNull();
  }

  @Test
  void callDuplicateCheck(BpmClient client) {
    MockOpenAI.defineChat(new SupplierDemoChat()::duplicateCheckResponse);
    var res = client.start().process(TEST_PROCESS.elementName("testCallDuplicateCheck")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getAgentResponse()).isNotNull();
  }

  @Test
  void validateFinancialPolicy(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testValidateFinancialPolicy")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFinancialValidationResult()).isNotNull();
  }
}
