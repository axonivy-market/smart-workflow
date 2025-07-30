package com.axonivy.utils.ai.tools;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.CallSignature;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification.Builder;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

@SuppressWarnings("restriction")
public class IvyToolSpecs {

  public static List<ToolSpecification> find() {
    return new IvyToolsProcesses(IProcessModelVersion.current())
        .toolStarts().stream()
        .map(IvyToolSpecs::toTool)
        .toList();
  }

  private static ToolSpecification toTool(CallSubStart toolStart) {
    CallSignature signature = toolStart.getSignature();
    Builder builder = ToolSpecification.builder()
        .name(signature.getName());
    if (StringUtils.isNotBlank(toolStart.getDescription())) {
      builder.description(toolStart.getDescription());
    }
    var params = JsonObjectSchema.builder();
    signature.getInputParameters().stream()
        .forEach(variable -> toJsonParam(variable, params));
    builder.parameters(params.build());

    return builder.build();
  }

  private static void toJsonParam(VariableDesc variable, JsonObjectSchema.Builder builder) {
    switch (variable.getType().getSimpleName()) {
      case "String":
        builder.addStringProperty(variable.getName(), variable.getInfo().getDescription());
        break;
      case "Integer":
        builder.addIntegerProperty(variable.getName(), variable.getInfo().getDescription());
        break;
      case "Number":
        builder.addNumberProperty(variable.getName(), variable.getInfo().getDescription());
        break;
      case "Boolean":
        builder.addBooleanProperty(variable.getName(), variable.getInfo().getDescription());
        break;
      default:
        // TODO native schema builder! see https://github.com/langchain4j/langchain4j/pull/3374
        break;
    }
  }
}
