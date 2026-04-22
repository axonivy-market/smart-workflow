package com.axonivy.utils.smart.workflow.tools.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class WebSearchIT {

  @Test
  void duckduckgo_reachable() {
    var engine = new DuckDuckGoSmartWebSearchEngine();
    var results = engine.search("axon ivy", 3);
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).title()).isNotBlank();
    assertThat(results.get(0).url()).isNotBlank();
  }
}
