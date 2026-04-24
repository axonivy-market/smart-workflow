package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;

public class DummySearchEngineProvider implements SmartWebSearchEngineProvider {

  @Override
  public List<SmartWebSearchEngine> getEngines() {
    return List.of(new DummySearchEngine());
  }
}
