package com.axonivy.utils.smart.workflow.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;
import com.axonivy.utils.smart.workflow.tools.math.MathToolChat;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.TraceSpan;
import ch.ivyteam.ivy.trace.Tracer;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestOpenInferenceSpans {
  
  private Tracer tracer;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new MathToolChat()::toolTest);

    fixture.var(OpenInferenceTracing.Var.ENABLED, "true");
    this.tracer = Tracer.instance();
    if (!this.tracer.isRunning()) {
      this.tracer.start();
    }
  }

  @Test
  void observesModelInteractions(BpmClient client) {
    var tools = BpmProcess.name("TestToolUser").elementName("math");
    var res = client.start().process(tools).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getSum()).isEqualTo(2025);

    var spans = tracer.slowTraces().all();
    var rootSpan = spans.get(0).rootSpan();
    var assistants = findChild(rootSpan, "AI Assistant").toList();
    assertThat(assistants).hasSize(2);

    var assistCalc = assistants.get(0);
    var calcAttrs = mapOf(assistCalc.attributes());
    assertIvyAttrs(calcAttrs, res.workflow().executedTask());
    assertToolCallAttrs(calcAttrs);

    var assistDone = assistants.get(1);
    var doneAttrs = mapOf(assistDone.attributes());
    assertIvyAttrs(doneAttrs, res.workflow().executedTask());
    assertToolDoneAttrs(doneAttrs);
    assertToolResult(doneAttrs);
  }

  private void assertToolCallAttrs(Map<String, String> attrs) {
    assertThat(attrs)
      .as("records tool call request attributes")
      .containsEntry("openinference.span.kind", "LLM")
      .containsEntry("llm.system", "langchain4j")
      .containsEntry("llm.provider", "openai")
      .containsEntry("llm.model_name", "gpt-4.1-mini")
      .containsEntry("input.mime_type", "application/json")
      .containsEntry("input.value", """
        [{"role":"system","content":"Use the 'add' tool to calculate"},{"role":"user","content":"Whats the sum of 1984 plus 41 ?"}]
        """.strip())
      .containsEntry("llm.invocation_parameters", "{}")
      .containsEntry("llm.input_messages.0.message.content", "SystemMessage { text = \"Use the 'add' tool to calculate\" }")
      .containsEntry("llm.input_messages.0.message.role", "system")
      .containsEntry("llm.input_messages.1.message.content", "UserMessage { name = null, contents = [TextContent { text = \"Whats the sum of 1984 plus 41 ?\" }], attributes = {} }")
      .containsEntry("llm.input_messages.1.message.role", "user");
        
    assertThat(attrs)
      .as("records tool schema")
      .containsEntry("llm.tools.0.tool.json_schema", """
          {"type":"function","function":{"name":"add","description":"This is a simple calculator: supporting additions of numbers","parameters":{"description":null,"properties":{"a":{"description":"first number"},"b":{"description":"number to be added"}},"required":[],"additionalProperties":null,"definitions":{}}}}
          """.trim());

    assertThat(attrs)
      .as("record tool call response attributes")
      .containsEntry("llm.output_messages.0.message.content", "null")
      .containsEntry("llm.output_messages.0.message.role", "assistant")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.function.arguments", "{\"a\":1984,\"b\":41}")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.function.name", "add")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.id", "call_rSF1CF9CDlXPzVykkgJTyRgU")
      .containsEntry("llm.response.finish_reasons", "[TOOL_EXECUTION]")
      .containsEntry("llm.token_count.completion", "18")
      .containsEntry("llm.token_count.prompt", "102")
      .containsEntry("llm.token_count.total", "120")
      .containsEntry("output.mime_type", "text/plain")
      .containsEntry("output.value", "null");
  }

  private void assertToolDoneAttrs(Map<String, String> attrs) {
    assertThat(attrs)
      .as("records completed tool-run attributes")
      .containsEntry("input.mime_type", "application/json")
      .containsEntry("llm.input_messages.0.message.role", "system")
      .containsEntry("llm.input_messages.0.message.content", 
      "SystemMessage { text = \"Use the 'add' tool to calculate\" }")
      .containsEntry("llm.input_messages.1.message.role", "user")
      .containsEntry("llm.input_messages.1.message.content", 
      "UserMessage { name = null, contents = [TextContent { text = \"Whats the sum of 1984 plus 41 ?\" }], attributes = {} }")
      .containsEntry("llm.input_messages.2.message.role", "assistant")
      .containsEntry("llm.input_messages.2.message.tool_calls.0.tool_call.function.arguments", "{\"a\":1984,\"b\":41}")
      .containsEntry("llm.input_messages.2.message.tool_calls.0.tool_call.function.name", "add")
      .containsEntry("llm.input_messages.2.message.tool_calls.0.tool_call.id", "call_rSF1CF9CDlXPzVykkgJTyRgU")
      .containsEntry("llm.input_messages.3.message.role", "tool")
      .containsEntry("llm.input_messages.3.message.tool_call_id", "call_rSF1CF9CDlXPzVykkgJTyRgU")
      .containsEntry("openinference.span.kind", "LLM")
      .containsEntry("llm.system", "langchain4j")
      .containsEntry("llm.provider", "openai")
      .containsEntry("llm.model_name", "gpt-4.1-mini")
      .containsEntry("llm.invocation_parameters", "{}")
      .containsEntry("llm.response.finish_reasons", "[STOP]")
      .containsEntry("llm.token_count.completion", "10")
      .containsEntry("llm.token_count.prompt", "138")
      .containsEntry("llm.token_count.total", "148")
      .containsEntry("output.mime_type", "text/plain")
      .containsEntry("output.value", "{\"value\":2025}");
  }

  private static void assertToolResult(Map<String, String> attrs) {
    assertThat(attrs)
      .as("records tool invocation arguments and result")
      .containsEntry("llm.output_messages.0.message.role", "assistant")
      .containsEntry("llm.output_messages.0.message.content", "{\"value\":2025}")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.function.name", "add")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.function.arguments",
          "{\"a\":1984,\"b\":41}")
      .containsEntry("llm.output_messages.0.message.tool_calls.0.tool_call.id", "call_rSF1CF9CDlXPzVykkgJTyRgU");
  }
  
  private static void assertIvyAttrs(Map<String,String> attrs, ITask task) {
    assertThat(attrs)
      .as("Traces are enriched with Ivy attributes")
      .containsEntry("ivy.case", task.getCase().uuid())
      .containsEntry("ivy.task", task.uuid());
  }

  private Stream<TraceSpan> findChild(TraceSpan span, String name) {
    var children = span.children();
    var names = children.stream().map(t -> t.name()).toList();
    assertThat(names).contains(name);
    return children.stream().filter(t -> t.name().equals(name));
  }

  private static Map<String, String> mapOf(List<Attribute> attributes) {
    return attributes.stream().collect(Collectors.toMap(Attribute::name, Attribute::value));
  }

}
