package com.axonivy.utils.smart.workflow.tools.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.tools.web.WebSearchTool.WebSearchToolResult;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestWebSearchTool {

  private final WebSearchTool tool = new WebSearchTool();

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(WebSearchTool.MAX_RESULTS, "");
    fixture.var(WhitelistDomainFilter.WHITELIST_DOMAINS, "");
    WebSearchCollector.setOverride(new DummySearchEngine());
    DummySearchEngine.setResults(List.of(
        new SmartWebSearchResult("Result 1", "https://example.com/page1", "snippet 1"),
        new SmartWebSearchResult("Result 2", "https://other.org/page2", "snippet 2"),
        new SmartWebSearchResult("Result 3", "https://example.com/page3", "snippet 3")));
  }

  @AfterEach
  void tearDown() {
    WebSearchCollector.setOverride(null);
    DummySearchEngine.reset();
  }

  @Test
  void metadata() {
    assertThat(tool.name()).isEqualTo("webSearch");
    assertThat(tool.description()).isNotBlank();
    assertThat(tool.parameters()).hasSize(1);
    assertThat(tool.parameters().get(0).name()).isEqualTo("query");
  }

  @Test
  void execute_returnsResults() {
    var result = (WebSearchToolResult) tool.execute(Map.of("query", "test"));
    assertThat(result.query()).isEqualTo("test");
    assertThat(result.results()).hasSize(3);
    assertThat(result.note()).isNull();
  }

  @Test
  void execute_whitelistFilters(AppFixture fixture) {
    fixture.var(WhitelistDomainFilter.WHITELIST_DOMAINS, "example.com");
    var result = (WebSearchToolResult) tool.execute(Map.of("query", "test"));
    assertThat(result.results())
        .hasSize(2)
        .extracting(SmartWebSearchResult::url)
        .allMatch(url -> url.contains("example.com"));
    assertThat(result.note()).isNull();
  }

  @Test
  void execute_whitelistExcludesAll(AppFixture fixture) {
    fixture.var(WhitelistDomainFilter.WHITELIST_DOMAINS, "nowhere.test");
    var result = (WebSearchToolResult) tool.execute(Map.of("query", "test"));
    assertThat(result.results()).isEmpty();
    assertThat(result.note()).contains("excluded by the configured domain whitelist");
  }

  @Test
  void execute_noResults() {
    DummySearchEngine.setResults(List.of());
    var result = (WebSearchToolResult) tool.execute(Map.of("query", "nothing"));
    assertThat(result.results()).isEmpty();
    assertThat(result.note()).contains("No results returned by the search engine");
  }

  @Test
  void maxResults_default() {
    assertThat(WebSearchTool.readMaxResults()).isEqualTo(5);
  }

  @Test
  void maxResults_configured(AppFixture fixture) {
    fixture.var(WebSearchTool.MAX_RESULTS, "10");
    assertThat(WebSearchTool.readMaxResults()).isEqualTo(10);
  }

  @Test
  void maxResults_invalid(AppFixture fixture) {
    fixture.var(WebSearchTool.MAX_RESULTS, "abc");
    assertThat(WebSearchTool.readMaxResults()).isEqualTo(5);
  }

  @Test
  void maxResults_limits(AppFixture fixture) {
    fixture.var(WebSearchTool.MAX_RESULTS, "2");
    var result = (WebSearchToolResult) tool.execute(Map.of("query", "test"));
    assertThat(result.results()).hasSize(2);
  }
}
