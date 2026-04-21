package com.axonivy.utils.smart.workflow.demo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.web.mock.WebSearchDemoChat;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.tools.web.DummySearchEngine;
import com.axonivy.utils.smart.workflow.tools.web.SmartWebSearchResult;
import com.axonivy.utils.smart.workflow.tools.web.WebSearchCollector;

import Features.WebSearchDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
public class TestWebSearchDemo {

  private static final BpmProcess WEB_SEARCH_DEMO = BpmProcess.name("WebSearchDemo");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("web-search-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    MockOpenAI.defineChat(new WebSearchDemoChat()::respond);
    WebSearchCollector.setOverride(new DummySearchEngine());
    DummySearchEngine.setResults(List.of(
        new SmartWebSearchResult("DuckDuckGo", "https://duckduckgo.com",
            "The Internet privacy company that empowers you to seamlessly take control of your personal information online."),
        new SmartWebSearchResult("How Search Engines Work", "https://example.com/how-search-works",
            "Search engines crawl, index, and rank web pages to provide relevant results.")));
  }

  @AfterEach
  void tearDown() {
    WebSearchCollector.setOverride(null);
    DummySearchEngine.reset();
  }

  @Test
  void webSearchProcess(BpmClient client) {
    var result = client.start()
        .process(WEB_SEARCH_DEMO.elementName("start"))
        .execute();

    WebSearchDemoData data = result.data().last();
    assertThat(data.getSearchSummary()).isNotNull();
    assertThat(data.getSearchSummary().getQuery()).isEqualTo("web search engine");
    assertThat(data.getSearchSummary().getSummary()).isNotBlank();
    assertThat(data.getSearchSummary().getResults()).isNotEmpty();
  }
}
