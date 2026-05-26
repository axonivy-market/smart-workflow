package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DummySearchEngine implements SmartWebSearchEngine {

  private static List<SmartWebSearchResult> RESULTS = new CopyOnWriteArrayList<>();

  @Override
  public String name() {
    return "dummy";
  }

  @Override
  public List<SmartWebSearchResult> search(String query, int maxResults) {
    return RESULTS.stream().limit(maxResults).toList();
  }

  public static void setResults(List<SmartWebSearchResult> results) {
    RESULTS = new CopyOnWriteArrayList<>(results);
  }

  public static void reset() {
    RESULTS = new CopyOnWriteArrayList<>();
  }
}
