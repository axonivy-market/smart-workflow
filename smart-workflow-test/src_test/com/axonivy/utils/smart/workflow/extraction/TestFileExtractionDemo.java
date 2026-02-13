package com.axonivy.utils.smart.workflow.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import Features.FileExtractionDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;

@RestResourceTest
class TestFileExtractionDemo {

  private static final BpmProcess FILE_EXTRACTION_DEMO = BpmProcess.name("FileExtractionDemo");

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("extraction"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(DefaultGuardrailProvider.DEFAULT_INPUT_GUARDRAILS, "");
    MockOpenAI.defineChat(request -> responder.send("response.json"));
  }

  @Test
  void extractInvoiceFromFiles(BpmClient client) {
    var result = client.start()
        .process(FILE_EXTRACTION_DEMO.elementName("extractFromImage"))
        .execute();

    FileExtractionDemoData data = result.data().last();
    assertThat(data.getInvoiceList()).isNotNull();
    assertThat(data.getInvoiceList().getInvoices()).hasSize(1);
    assertThat(data.getInvoiceList().getInvoices().get(0).getInvoiceNumber()).isEqualTo("INV-101");
    assertThat(data.getInvoiceList().getInvoices().get(0).getCustomerName()).isEqualTo("Acme Corp");
    assertThat(data.getInvoiceList().getInvoices().get(0).getTotalAmount()).isEqualTo(100.00);
    assertThat(data.getInvoiceList().getInvoices().get(0).getItems()).hasSize(1);
  }
}
