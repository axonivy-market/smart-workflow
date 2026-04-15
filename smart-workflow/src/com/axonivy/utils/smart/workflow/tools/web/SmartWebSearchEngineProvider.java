package com.axonivy.utils.smart.workflow.tools.web;

import java.util.Collections;
import java.util.List;

public interface SmartWebSearchEngineProvider {

  default String name() {
    return getClass().getSimpleName();
  }

  default List<SmartWebSearchEngine> getEngines() {
    return Collections.emptyList();
  }
}
