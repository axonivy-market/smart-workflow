package com.axonivy.utils.smart.workflow.rag.tool;

import java.util.List;

import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

public class OpenSearchRagToolProvider implements SmartWorkflowToolsProvider {

  @Override
  public List<SmartWorkflowTool> getTools() {
    return List.of(new OpenSearchRagTool());
  }
}
