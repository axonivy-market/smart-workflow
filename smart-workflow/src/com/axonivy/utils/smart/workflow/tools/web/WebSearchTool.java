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
    List<SmartWebSearchResult> rawResults = engine.search(query, readMaxResults());
    List<SmartWebSearchResult> filteredResults = new WhitelistDomainFilter().filter(rawResults);
    return new WebSearchToolResult(query, filteredResults, noteFor(rawResults, filteredResults));
  }

  private static String noteFor(List<SmartWebSearchResult> raw, List<SmartWebSearchResult> filtered) {
    if (!filtered.isEmpty()) {
      return null;
    }
    if (raw.isEmpty()) {
      return "No results returned by the search engine. The source may be rate-limited or the query may have no indexed matches. Do not speculate about the cause; report to the user that no results were available.";
    }
    return "All " + raw.size() + " result(s) were excluded by the configured domain whitelist.";
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

  public record WebSearchToolResult(String query, List<SmartWebSearchResult> results, String note) {}
}
