package com.axonivy.utils.smart.workflow.tools.adapter;

import java.util.Map;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.tools.internal.JsonProcessParameters;
import com.axonivy.utils.smart.workflow.tools.internal.JsonToolParamBuilder;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.Json;
import dev.langchain4j.service.tool.ToolExecutor;

public class JavaToolAdapter {

  private final SmartWorkflowTool tool;
  private final JsonToolParamBuilder paramBuilder;
  private final JsonProcessParameters paramProcessor;

  public JavaToolAdapter(SmartWorkflowTool tool) {
    this.tool = tool;

    var classLoader = tool.getClass().getClassLoader();
    this.paramBuilder = new JsonToolParamBuilder(classLoader);
    this.paramProcessor = new JsonProcessParameters(classLoader);

  }

  public ToolSpecification toToolSpecification() {
    return ToolSpecification.builder()
        .name(Optional.ofNullable(tool).map(SmartWorkflowTool::name).orElse("unknown"))
        .description(Optional.ofNullable(tool).map(SmartWorkflowTool::description).orElse(""))
        .parameters(paramBuilder.toParams(Optional.ofNullable(tool).map(SmartWorkflowTool::parameters).orElse(null)))
        .build();
  }

  public ToolExecutor toToolExecutor() {
    return (request, memoryId) -> {
      try {
        Map<String, Object> args = paramProcessor.readParams(tool.parameters(), request.arguments());
        Object result = tool.execute(args);
        return (result instanceof String s) ? s : Json.toJson(result);
      } catch (Exception e) {
        throw new RuntimeException(
            "Error executing tool '%s' with args: %s".formatted(tool.name(), request.arguments()), e);
      }
    };
  }
}
