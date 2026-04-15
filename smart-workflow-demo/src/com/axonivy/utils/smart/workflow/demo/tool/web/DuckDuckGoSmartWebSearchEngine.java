package com.axonivy.utils.smart.workflow.demo.tool.web;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.tools.web.SmartWebSearchEngine;
import com.axonivy.utils.smart.workflow.tools.web.SmartWebSearchResult;

import dev.langchain4j.community.web.search.duckduckgo.DuckDuckGoWebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;

public class DuckDuckGoSmartWebSearchEngine implements SmartWebSearchEngine {

  private final DuckDuckGoWebSearchEngine searchEngine;

  public DuckDuckGoSmartWebSearchEngine() {
    this.searchEngine = DuckDuckGoWebSearchEngine.builder()
        .duration(Duration.ofSeconds(10))
        .build();
  }

  @Override
  public String name() {
    return "duckduckgo";
  }

  @Override
  public List<SmartWebSearchResult> search(String query, int maxResults) {
    var results = searchEngine.search(WebSearchRequest.from(query, maxResults));
    return results.results().stream()
        .map(r -> new SmartWebSearchResult(
            r.title(),
            r.url().toString(),
            r.snippet() != null ? r.snippet() : ""))
        .collect(Collectors.toList());
  }
}
