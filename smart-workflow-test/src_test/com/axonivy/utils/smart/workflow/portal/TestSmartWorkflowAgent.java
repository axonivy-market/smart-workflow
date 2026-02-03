package com.axonivy.utils.smart.workflow.portal;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.fasterxml.jackson.databind.JsonNode;

import Features.SmartWorkflowAgentDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@RestResourceTest
class TestSmartWorkflowAgent {

  private static final BpmProcess SMART_WORKFLOW_AGENT_DEMO = BpmProcess.name("SmartWorkflowAgentDemo");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("invoice"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -> invoiceResponse(request, responder));
  }

  private Response invoiceResponse(JsonNode request, ResourceResponder responder) {
    return responder.send("response.json");
  }

  @Test
  void testInvokeAgent(BpmClient client) {
    var result = client.start()
        .process(SMART_WORKFLOW_AGENT_DEMO.elementName("start"))
        .execute();

    SmartWorkflowAgentDemoData data = result.data().last();
    assertThat(data.getInvoice()).isNotNull();
    assertThat(data.getInvoice().getInvoiceNumber()).isEqualTo("INV-001");
    assertThat(data.getInvoice().getCustomerName()).isEqualTo("John Doe");
    assertThat(data.getInvoice().getTotalAmount()).isEqualTo(300.00);
    assertThat(data.getInvoice().getItems()).hasSize(2);
  }
}
