package com.axonivy.utils.smart.workflow.tools.provider;

import java.util.List;

import com.axonivy.utils.smart.workflow.tools.web.WebSearchTool;

public class BuiltinToolsProvider implements SmartWorkflowToolsProvider {

  @Override
  public List<SmartWorkflowTool> getTools() {
    return List.of(new WebSearchTool());
  }
}
