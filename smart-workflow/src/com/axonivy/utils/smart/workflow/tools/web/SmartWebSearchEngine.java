package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;

public interface SmartWebSearchEngine {

  default String name() {
    return getClass().getSimpleName();
  }

  List<SmartWebSearchResult> search(String query, int maxResults);
}
