package com.axonivy.utils.ai.tools;

import java.util.HashMap;
import java.util.Map;

import com.axonivy.utils.ai.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.ai.tools.internal.IvySubProcessToolSpecs;
import com.axonivy.utils.ai.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

public class IvySubProcessToolsProvider implements ToolProvider {

  @Override
  public ToolProviderResult provideTools(ToolProviderRequest provide) {
    ToolExecutor executor = (request, memoryId) -> IvySubProcessToolExecutor.execute(request).text(); // TODO; user centric memory interpretation!
    Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
    new IvyToolsProcesses(IProcessModelVersion.current())
        .toolStarts().stream().forEach(start -> tools.put(IvySubProcessToolSpecs.toTool(start), executor));
    return new ToolProviderResult(tools);
  }

}
