package com.axonivy.utils.smart.workflow.tools.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestWhitelistDomainFilter {

  @Test
  void emptyWhitelist_passesAll() {
    var filter = new WhitelistDomainFilter(Set.of());
    var results = List.of(
        result("https://example.com/page"),
        result("https://other.org/docs"));
    assertThat(filter.filter(results)).hasSize(2);
  }

  @Test
  void exactDomainMatch() {
    var filter = new WhitelistDomainFilter(Set.of("example.com"));
    var results = List.of(
        result("https://example.com/page"),
        result("https://other.org/docs"));
    assertThat(filter.filter(results))
        .hasSize(1)
        .first()
        .extracting(SmartWebSearchResult::url)
        .isEqualTo("https://example.com/page");
  }

  @Test
  void subdomainMatch() {
    var filter = new WhitelistDomainFilter(Set.of("example.com"));
    var results = List.of(
        result("https://docs.example.com/guide"),
        result("https://blog.example.com/post"));
    assertThat(filter.filter(results)).hasSize(2);
  }

  @Test
  void noSubdomainFalsePositive() {
    var filter = new WhitelistDomainFilter(Set.of("example.com"));
    var results = List.of(result("https://notexample.com/page"));
    assertThat(filter.filter(results)).isEmpty();
  }

  @Test
  void multipleDomains() {
    var filter = new WhitelistDomainFilter(Set.of("example.com", "docs.oracle.com"));
    var results = List.of(
        result("https://example.com/page"),
        result("https://docs.oracle.com/javase"),
        result("https://stackoverflow.com/q"));
    assertThat(filter.filter(results))
        .hasSize(2)
        .extracting(SmartWebSearchResult::url)
        .containsExactly("https://example.com/page", "https://docs.oracle.com/javase");
  }

  @Test
  void malformedUrl_filtered() {
    var filter = new WhitelistDomainFilter(Set.of("example.com"));
    var results = List.of(result("not-a-url"));
    assertThat(filter.filter(results)).isEmpty();
  }

  @Test
  void nullHost_filtered() {
    var filter = new WhitelistDomainFilter(Set.of("example.com"));
    var results = List.of(result("mailto:user@example.com"));
    assertThat(filter.filter(results)).isEmpty();
  }

  private static SmartWebSearchResult result(String url) {
    return new SmartWebSearchResult("title", url, "snippet");
  }
}
