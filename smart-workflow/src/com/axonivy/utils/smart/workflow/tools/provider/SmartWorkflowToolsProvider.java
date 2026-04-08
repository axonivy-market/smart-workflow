package com.axonivy.utils.smart.workflow.tools.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;
import com.axonivy.utils.smart.workflow.tools.adapter.JavaToolAdapter;
import com.axonivy.utils.smart.workflow.tools.entity.SmartWorkflowTool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderResult;

public interface SmartWorkflowToolsProvider {

  default String name() { return getClass().getSimpleName(); }

  List<SmartWorkflowTool> getTools();

  static ToolProviderResult provideTools() {
    Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
    var project = SpiProject.getSmartWorkflowPmv().project();
    new SpiLoader(project).load(SmartWorkflowToolsProvider.class)
        .stream()
        .flatMap(provider -> provider.getTools().stream())
        .map(JavaToolAdapter::new)
        .forEach(adapter -> tools.put(adapter.toToolSpecification(), adapter.toToolExecutor()));
    return new ToolProviderResult(tools);
  }
}
