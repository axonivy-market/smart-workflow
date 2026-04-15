package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

public class WebSearchTool implements SmartWorkflowTool {

  private static final int DEFAULT_MAX_RESULTS = 5;

  @Override
  public String name() {
    return "webSearch";
  }

  @Override
  public String description() {
    return """
        Search the web for current information using the given query.
        Returns a list of search results with titles, URLs, and content snippets.
        Use this tool to find up-to-date or factual information from the internet.""";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        new ToolParameter("query",
            "The search query to look up on the web",
            "java.lang.String"));
  }

  @Override
  public Object execute(Map<String, Object> args) {
    var engine = WebSearchCollector.findEngine()
        .orElseThrow(() -> new IllegalStateException(
            "No SmartWebSearchEngine found. Register a SmartWebSearchEngineProvider via META-INF/services."));
    String query = (String) args.get("query");
    List<SmartWebSearchResult> results = engine.search(query, DEFAULT_MAX_RESULTS);
    return new WebSearchToolResult(query, results);
  }

  public record WebSearchToolResult(String query, List<SmartWebSearchResult> results) {}
}
