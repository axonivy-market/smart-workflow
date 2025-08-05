package com.axonivy.utils.ai.mapper;

import java.util.function.Function;
import java.util.stream.Collectors;

import com.axonivy.utils.ai.core.tool.IvyTool;

import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;

public class IvyToolMapper {
  @SuppressWarnings("restriction")
  public static final Function<CallSubStart, IvyTool> fromCallSubStart = tool -> {
    IvyTool ivyTool = new IvyTool();
    ivyTool.setSignature(tool.getSignature().toSignatureString());
    ivyTool.setId(tool.getBpmnId());
    ivyTool.setName(tool.getName());
    ivyTool.setIvyProcess(tool);

    ivyTool.setParameters(tool.getSignature().getInputParameters().stream().map(IvyToolParameterMapper.fromVariableDesc)
        .collect(Collectors.toList()));

    ivyTool.setResultDefinitions(tool.getSignature().getOutputParameters().stream()
        .map(IvyToolParameterMapper.fromVariableDesc).collect(Collectors.toList()));

    return ivyTool;
  };

}
