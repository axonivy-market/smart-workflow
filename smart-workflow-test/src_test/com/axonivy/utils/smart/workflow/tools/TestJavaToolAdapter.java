package com.axonivy.utils.smart.workflow.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.tools.adapter.JavaToolAdapter;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool.ToolParameter;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

@IvyTest
class TestJavaToolAdapter {

  static class Greeter implements SmartWorkflowTool {
    @Override
    public String description() { return "greet a user by name"; }

    @Override
    public List<ToolParameter> parameters() {
      return List.of(new ToolParameter("name", "the user's name", "java.lang.String"));
    }

    @Override
    public Object execute(Map<String, Object> args) {
      return "Hello, " + args.get("name") + "!";
    }
  }

  static class ObjectReturner implements SmartWorkflowTool {
    @Override
    public String description() { return "echo input"; }

    @Override
    public List<ToolParameter> parameters() { return List.of(); }

    @Override
    public Object execute(Map<String, Object> args) { return Map.of("key", "val"); }
  }

  @Test
  void toToolSpecification() {
    var spec = new JavaToolAdapter(new Greeter()).toToolSpecification();
    assertThat(spec.name()).isEqualTo("Greeter");
    assertThat(spec.description()).isEqualTo("greet a user by name");
    var schema = (JsonObjectSchema) spec.parameters();
    assertThat(schema.properties().get("name")).isInstanceOf(JsonStringSchema.class);
    assertThat(schema.required()).containsExactly("name");
  }

  @Test
  void noParameters() {
    var spec = new JavaToolAdapter(new ObjectReturner()).toToolSpecification();
    assertThat(spec.parameters()).isNull();
  }

  @Test
  void executorReturnsString() throws Exception {
    var executor = new JavaToolAdapter(new Greeter()).toToolExecutor();
    var request = ToolExecutionRequest.builder()
        .name("Greeter")
        .arguments("{\"name\": \"Alice\"}")
        .build();
    assertThat(executor.execute(request, null)).isEqualTo("Hello, Alice!");
  }

  @Test
  void executorSerializesObjectAsJson() throws Exception {
    var executor = new JavaToolAdapter(new ObjectReturner()).toToolExecutor();
    var request = ToolExecutionRequest.builder()
        .name("ObjectReturner")
        .arguments("{}")
        .build();
    var result = executor.execute(request, null);
    assertThat(result).contains("\"key\"").contains("\"val\"");
  }
}
