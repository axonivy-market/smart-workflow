package com.axonivy.utils.ai.core.internal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.CallSignature;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification.Builder;

@SuppressWarnings("restriction")
public class IvySubProcessToolSpecs {

  public static List<ToolSpecification> find() {
    return new IvyToolsProcesses(IProcessModelVersion.current())
        .toolStarts().stream()
        .map(IvySubProcessToolSpecs::toTool)
        .toList();
  }

  public static ToolSpecification toTool(CallSubStart toolStart) {
    CallSignature signature = toolStart.getSignature();
    Builder builder = ToolSpecification.builder()
        .name(signature.getName());
    if (StringUtils.isNotBlank(toolStart.getDescription())) {
      builder.description(toolStart.getDescription());
    }

    var params = new JsonToolParamBuilder(IProcessModelVersion.current())
        .toParams(signature.getInputParameters());
    builder.parameters(params);

    return builder.build();
  }

}
