package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.call.SubProcessCallStart;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification.Builder;

@SuppressWarnings("restriction")
public class IvySubProcessToolSpecs {

  public static List<ToolSpecification> find() {
    return IvyToolsProcesses.toolStarts().stream()
        .map(IvySubProcessToolSpecs::toTool)
        .toList();
  }

  public static ToolSpecification toTool(SubProcessCallStart toolStart) {
    var method = toolStart.description();
    Builder builder = ToolSpecification.builder()
        .name(method.name());
    if (StringUtils.isNotBlank(method.description())) {
      builder.description(method.description());
    }

    var params = new JsonToolParamBuilder(IProcessModelVersion.current())
        .toParams(method.in());
    builder.parameters(params);

    return builder.build();
  }

}
