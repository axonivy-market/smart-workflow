package com.axonivy.utils.smart.workflow.tools.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.smart.workflow.tools.internal.IvySubProcessToolSpecs;
import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

public class IvySubProcessToolsProvider implements ToolProvider {

  private List<String> toolFilter = null;

  public IvySubProcessToolsProvider filtering(List<String> toolFilters) {
    this.toolFilter = toolFilters;
    return this;
  }

  @Override
  public ToolProviderResult provideTools(ToolProviderRequest provide) {
    return getTools(new IvyToolsProcesses().toolStarts());
  }

  public ToolProviderResult getTools(List<SubProcessCallStartEvent> starts) {
    Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
    starts.stream()
        .filter(start -> toolFilter == null || toolFilter.contains(start.description().name()))
        .forEach(start -> tools.put(IvySubProcessToolSpecs.toTool(start), executorFor(start)));
    return new ToolProviderResult(tools);
  }

  private static ToolExecutor executorFor(SubProcessCallStartEvent start) {
    return (request, _) -> IvySubProcessToolExecutor.execute(request, start).text();
  }

}
