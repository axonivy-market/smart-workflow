package com.axonivy.utils.smart.workflow.demo.tool;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

import dev.langchain4j.community.web.search.duckduckgo.DuckDuckGoWebSearchEngine;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;

public class WebSearchTool implements SmartWorkflowTool {

  private static final int DEFAULT_MAX_RESULTS = 5;

  private final WebSearchEngine searchEngine;

  public WebSearchTool() {
    this.searchEngine = DuckDuckGoWebSearchEngine.builder()
        .duration(Duration.ofSeconds(10))
        .build();
  }

  public record SearchResult(String title, String url, String snippet) {}

  public record WebSearchToolResult(String query, List<SearchResult> results) {}

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
    String query = (String) args.get("query");
    WebSearchResults searchResults = searchEngine.search(
        WebSearchRequest.from(query, DEFAULT_MAX_RESULTS));

    List<SearchResult> results = searchResults.results().stream()
        .map(r -> new SearchResult(
            r.title(),
            r.url().toString(),
            r.snippet() != null ? r.snippet() : ""))
        .collect(Collectors.toList());

    return new WebSearchToolResult(query, results);
  }
}
