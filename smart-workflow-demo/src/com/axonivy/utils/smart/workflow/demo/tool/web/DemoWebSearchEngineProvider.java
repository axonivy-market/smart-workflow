package com.axonivy.utils.smart.workflow.demo.tool.web;

import java.util.List;

import com.axonivy.utils.smart.workflow.tools.web.SmartWebSearchEngine;
import com.axonivy.utils.smart.workflow.tools.web.SmartWebSearchEngineProvider;

public class DemoWebSearchEngineProvider implements SmartWebSearchEngineProvider {

  @Override
  public List<SmartWebSearchEngine> getEngines() {
    return List.of(new DuckDuckGoSmartWebSearchEngine());
  }
}
