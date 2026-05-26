package com.axonivy.utils.smart.workflow.tools.provider;

import java.util.List;
import java.util.Map;

public interface SmartWorkflowTool {

  record ToolParameter(String name, String description, String type) {}

  String description();

  List<ToolParameter> parameters();

  Object execute(Map<String, Object> args);

  default String name() {
    return getClass().getSimpleName();
  }
}
