package com.axonivy.utils.smart.workflow.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.support.mock.SupportToolChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing;

import AgentDemo.SupportAgentToolsData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Tracer;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestOpenInferenceSpans {
  
  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("SupportAgentTools");
  private Tracer tracer;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new SupportToolChat()::toolTest);

    fixture.var(OpenInferenceTracing.Var.ENABLED, "true");
    this.tracer = Tracer.instance();
    if (!this.tracer.isRunning()) {
      this.tracer.start();
    }
  }

  @Test
  void observesModelInteractions(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsData) res.data().onElement(BpmElement.pid("19856884121ED111-f1"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");

    var spans =  tracer.slowTraces().all();
    var children = spans.get(0).rootSpan().children();
    var names = children.stream().map(t -> t.name()).toList();
    assertThat(names).contains("AI Assistant");
    var assistant = children.stream().filter(t -> t.name().equals("AI Assistant")  ).findFirst();
    var attrs = assistant.get().attributes().stream().collect(Collectors.toMap(Attribute::name, Attribute::value));

    assertIvyAttrs(attrs, res.workflow().executedTask());
    assertInputAttrs(attrs);
    assertOutputAttrs(attrs);
  }

  private void assertIvyAttrs(Map<String,String> attrs, ITask task) {
    assertThat(attrs)
      .as("Traces are enriched with Ivy attributes")
      .containsEntry("ivy.case", task.getCase().uuid())
      .containsEntry("ivy.task", task.uuid());
  }

  private void assertInputAttrs(Map<String, String> attrs) {
    var expectedInputValue = """
        [{"role":"system","content":"You are a Support Agent"},{"role":"user","content":"I have error 404 in Cockpit"}]
""".strip();

    assertThat(attrs)
      .as("Openinference input attributes")
      .containsEntry("openinference.span.kind", "LLM")
      .containsEntry("input.mime_type", "application/json")
      .containsEntry("input.value", expectedInputValue)
      .containsEntry("llm.system", "langchain4j")
      .containsEntry("llm.provider", "openai")
      .containsEntry("llm.model_name", "gpt-4.1-mini")
      .containsEntry("llm.input_messages.0.message.content", "SystemMessage { text = \"You are a Support Agent\" }")
      .containsEntry("llm.input_messages.0.message.role", "system")
      .containsEntry("llm.input_messages.1.message.content", "UserMessage { name = null, contents = [TextContent { text = \"I have error 404 in Cockpit\" }], attributes = {} }")
      .containsEntry("llm.input_messages.1.message.role", "user");
  }

  private void assertOutputAttrs(Map<String, String> attrs) {
    var expectedTicketJson = """
        {"id":"1","type":"TECHNICAL","name":"Support ticket: Error 404 in Cockpit","description":"User reports encountering a 404 error when accessing Cockpit. Needs investigation to identify the cause and resolve the issue.","employeeUsername":"user","firstApprover":"","secondApprover":"","aiApproval":{"decision":"WARNING","reason":"The issue is a technical error that requires further investigation by the technical team."},"firstApproval":{},"secondApproval":{},"requestor":{"username":"user","fullName":"User","position":"JUNIOR","departmentId":"dept1","maxLeaveDays":20,"usedLeaveDays":5,"email":"user@example.com","department":{"id":"dept1","name":"IT","firstLevelManager":"manager1","secondLevelManager":"manager2","firstLevelManagerEmp":{"username":"manager1","fullName":"Manager One","position":"MANAGER","departmentId":"dept1","maxLeaveDays":30,"usedLeaveDays":10,"email":"manager1@example.com","department":{}},"secondLevelManagerEmp":{"username":"manager2","fullName":"Manager Two","position":"MANAGER","departmentId":"dept1","maxLeaveDays":30,"usedLeaveDays":10,"email":"manager2@example.com","department":{}}}}}
        """
        .strip();

    assertThat(attrs)
        .as("Openinference output attributes")
        .containsEntry("llm.output_messages.0.message.content", expectedTicketJson)
        .containsEntry("llm.output_messages.0.message.role", "assistant")
        .containsEntry("llm.response.finish_reasons", "[STOP]")
        .containsEntry("llm.token_count.completion", "266")
        .containsEntry("llm.token_count.prompt", "971")
        .containsEntry("llm.token_count.total", "1237")
        .containsEntry("output.mime_type", "text/plain")
        .containsEntry("output.value", expectedTicketJson);
  }
}
