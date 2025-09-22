package com.axonivy.utils.smart.workflow.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.smart.workflow.tools.internal.IvySubProcessToolSpecs;
import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

public class IvySubProcessToolsProvider implements ToolProvider {

  private List<String> toolFilter = null;

  public IvySubProcessToolsProvider filtering(List<String> toolFilter) {
    this.toolFilter = toolFilter;
    return this;
  }

  @Override
  public ToolProviderResult provideTools(ToolProviderRequest provide) {
    ToolExecutor executor = (request, memoryId) -> IvySubProcessToolExecutor.execute(request).text(); // TODO; user centric memory interpretation!
    Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
    new IvyToolsProcesses(IProcessModelVersion.current())
        .toolStarts().stream()
        .map(IvySubProcessToolSpecs::toTool)
        .filter(spec -> toolFilter == null || toolFilter.contains(spec.name()))
        .forEach(spec -> tools.put(spec, executor));
    return new ToolProviderResult(tools);
  }

}
