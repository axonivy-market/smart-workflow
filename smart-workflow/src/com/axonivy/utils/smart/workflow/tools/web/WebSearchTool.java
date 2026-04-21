package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

import ch.ivyteam.ivy.environment.Ivy;

public class WebSearchTool implements SmartWorkflowTool {

  private static final int DEFAULT_MAX_RESULTS = 5;
  public static final String MAX_RESULTS = "AI.Tool.WebSearch.MaxResults";

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
    List<SmartWebSearchResult> results = engine.search(query, readMaxResults());
    results = new WhitelistDomainFilter().filter(results);
    return new WebSearchToolResult(query, results);
  }

  static int readMaxResults() {
    var configuredValue = StringUtils.defaultString(Ivy.var().get(MAX_RESULTS)).strip();
    if (configuredValue.isEmpty()) {
      return DEFAULT_MAX_RESULTS;
    }
    try {
      return Integer.parseInt(configuredValue);
    } catch (NumberFormatException e) {
      return DEFAULT_MAX_RESULTS;
    }
  }

  public record WebSearchToolResult(String query, List<SmartWebSearchResult> results) {}
}
