package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification.Builder;

@SuppressWarnings("restriction")
public class IvySubProcessToolSpecs {

  public static List<ToolSpecification> find() {
    return IvyToolsProcesses.toolStarts().stream()
        .map(IvySubProcessToolSpecs::toTool)
        .toList();
  }

  public static ToolSpecification toTool(SubProcessCallStartEvent toolStart) {
    var method = toolStart.description();
    Builder builder = ToolSpecification.builder()
        .name(method.name());
    if (StringUtils.isNotBlank(method.description())) {
      builder.description(method.description());
    }

    var params = new JsonToolParamBuilder().toParams(method.in());
    builder.parameters(params);

    return builder.build();
  }

}
