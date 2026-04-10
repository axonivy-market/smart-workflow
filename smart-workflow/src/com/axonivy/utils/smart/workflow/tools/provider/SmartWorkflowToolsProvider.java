package com.axonivy.utils.smart.workflow.tools.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;
import com.axonivy.utils.smart.workflow.tools.adapter.JavaToolAdapter;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderResult;

public interface SmartWorkflowToolsProvider {

  default String name() { return getClass().getSimpleName(); }

  List<SmartWorkflowTool> getTools();

  static ToolProviderResult provideTools(List<String> toolFilter) {
    Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
    var project = SpiProject.getSmartWorkflowPmv().project();
    new SpiLoader(project).load(SmartWorkflowToolsProvider.class)
        .stream()
        .flatMap(provider -> {
          var toolList = provider.getTools();
          return toolList == null ? Stream.empty() : toolList.stream();
        })
        .filter(tool -> toolFilter == null || toolFilter.contains(tool.name()))
        .map(JavaToolAdapter::new)
        .forEach(adapter -> tools.put(adapter.toToolSpecification(), adapter.toToolExecutor()));
    return new ToolProviderResult(tools);
  }
}
