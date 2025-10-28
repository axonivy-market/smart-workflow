package com.axonivy.utils.smart.workflow.tools.websearch;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.community.web.search.duckduckgo.DuckDuckGoWebSearchEngine;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilderLoader;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.WebSearchTool;

@RestResourceTest
class TestWebSearchTool {

  @BeforeEach
  void setup(AppFixture fixture) {
    // fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    // fixture.var(OpenAiConf.API_KEY, "");
    // MockOpenAI.defineChat(new SupportToolChat()::toolTest);
  }

  @Test
  void searchWeb() throws Exception {
    var duckDuckGo = DuckDuckGoWebSearchEngine.builder()
        .logRequests(true)
        .logResponses(true)
        .build();
    var searchTool = WebSearchTool.from(duckDuckGo);

    OpenAiChatModel model = OpenAiTestClient.aiMock();
    var assistant = AiServices.builder(WebAssist.class)
        .chatModel(model)
        .tools(searchTool)
        .build();

    String result = assistant.chat("site:market.axonivy.com connector for outlook");
    assertThat(result).isNotNull();
  }

  @Test
  void searchMarket() throws Exception {
    var market = new MarketSearch();
    var searchTool = new MarketTool(market);

    OpenAiChatModel model = OpenAiTestClient.aiMock();
    var assistant = AiServices.builder(WebAssist.class)
        .chatModel(model)
        .tools(searchTool)
        .build();

    String result = assistant.chat("connector for outlook");
    assertThat(result).isNotNull();
  }

  private static class MarketTool {

    private final MarketSearch searchEngine;

    public MarketTool(MarketSearch searchEngine) {
      this.searchEngine = searchEngine;
    }

    @Tool("This tool can be used to search for connectors to third-party vendors")
    public String searchWeb(@P("Web search query") String query, String product, String vendor) {
      WebSearchResults results = searchEngine.search(vendor + " " + product);
      return format(results);
    }

    private String format(WebSearchResults results) {
      if (isNullOrEmpty(results.results())) {
        return "No results found.";
      }

      return results.results()
          .stream()
          .map(organicResult -> "Title: " + organicResult.title() + "\n"
              + "Source: " + organicResult.url().toString() + "\n"
              + (organicResult.content() != null ? "Content:" + "\n" + organicResult.content() : "Snippet:" + "\n" + organicResult.snippet()))
          .collect(Collectors.joining("\n\n"));
    }
  }

  private static class MarketSearch implements WebSearchEngine {

    private final HttpClient client;

    public MarketSearch() {
      this.client = client();
    }

    private static HttpClient client() {
      var client = HttpClientBuilderLoader.loadHttpClientBuilder().build();
      return new LoggingHttpClient(client, true, true);
    }

    @Override
    public WebSearchResults search(WebSearchRequest request) {
      // TODO Auto-generated method stub
      var terms = request.searchTerms().split(" ");
      HttpRequest httpRequest = HttpRequest.builder()
          .method(HttpMethod.GET)
          .url("https://market.axonivy.com/marketplace-service/api/product?"
              + "type=all&sort=standard&language=en&page=0&size=20&isRESTClient=true&keyword=" + terms[0])
          .build();
      var response = client.execute(httpRequest);
      System.out.println(response.body());

      var fake = WebSearchOrganicResult.from("y", URI.create("https://market.axonivy.com"), "b", "c");
      return WebSearchResults.from(WebSearchInformationResult.from(1l), List.of(fake));
    }

  }

  private interface WebAssist {
    String chat(String query);
  }

}
