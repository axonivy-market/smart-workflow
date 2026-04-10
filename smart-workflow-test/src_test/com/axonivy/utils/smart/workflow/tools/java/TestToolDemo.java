package com.axonivy.utils.smart.workflow.tools.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.tools.java.mock.ToolDemoChat;

import Features.ToolDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
public class TestToolDemo {

  private static final BpmProcess TOOL_DEMO = BpmProcess.name("ToolDemo");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(DefaultGuardrailProvider.DEFAULT_INPUT_GUARDRAILS, "");
    MockOpenAI.defineChat(new ToolDemoChat()::respond);
  }

  @Test
  void calculateInvoiceTax(BpmClient client) {
    var result = client.start()
        .process(TOOL_DEMO.elementName("start"))
        .execute();

    ToolDemoData data = result.data().last();
    var taxedInvoice = data.getTaxedInvoice();

    assertThat(taxedInvoice).isNotNull();
    assertThat(taxedInvoice.getInvoiceNumber()).isEqualTo("INV-2024-042");
    assertThat(taxedInvoice.getCustomerName()).isEqualTo("John Doe");
    assertThat(taxedInvoice.getItems()).hasSize(5);
    assertThat(taxedInvoice.getSubtotal()).isEqualTo(13343.0);
    assertThat(taxedInvoice.getTaxAmount()).isCloseTo(2172.05, within(0.01));
    assertThat(taxedInvoice.getTotalWithTax()).isCloseTo(15515.05, within(0.01));

    var macBook = taxedInvoice.getItems().get(0);
    assertThat(macBook.getDescription()).isEqualTo("MacBook Pro 16-inch");
    assertThat(macBook.getTaxRate()).isCloseTo(0.1, within(0.001));
    assertThat(macBook.getTaxAmount()).isCloseTo(249.9, within(0.01));

    var rolex = taxedInvoice.getItems().get(3);
    assertThat(rolex.getDescription()).isEqualTo("Rolex Submariner Watch");
    assertThat(rolex.getTaxRate()).isCloseTo(0.2, within(0.001));
    assertThat(rolex.getTaxAmount()).isCloseTo(1700.0, within(0.01));
  }
}
