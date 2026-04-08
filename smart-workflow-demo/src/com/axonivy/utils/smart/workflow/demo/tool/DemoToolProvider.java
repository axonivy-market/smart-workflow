package com.axonivy.utils.smart.workflow.demo.tool;

import java.util.List;

import com.axonivy.utils.smart.workflow.tools.entity.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

public class DemoToolProvider implements SmartWorkflowToolsProvider {

  @Override
  public List<SmartWorkflowTool> getTools() {
    return List.of(new TaxCalculatorTool());
  }
}
