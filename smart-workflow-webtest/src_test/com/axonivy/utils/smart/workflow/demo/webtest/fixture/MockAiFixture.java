package com.axonivy.utils.smart.workflow.demo.webtest.fixture;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.ivy.webtest.engine.WebAppFixture;
import com.axonivy.utils.smart.workflow.demo.webtest.mock.WebTestMockProvider;
import com.axonivy.utils.smart.workflow.demo.webtest.mock.WebTestMockRule;
import com.axonivy.utils.smart.workflow.demo.webtest.mock.WebTestMockRules;

import tools.jackson.databind.ObjectMapper;

/**
 * Lets a web test define how the AI answers: every agent then uses {@link WebTestMockProvider}, which answers with
 * the rules given here, so no request reaches a real model.
 */
public interface MockAiFixture {

  String DEFAULT_PROVIDER = "AI.DefaultProvider";

  /** selects the mock model for every agent; a request then fails until {@link #mockAi(WebTestMockRule...)} */
  default void useMockAi(WebAppFixture fixture) {
    fixture.var(DEFAULT_PROVIDER, WebTestMockProvider.NAME);
    clearMockAi();
  }

  default void resetMockAi(WebAppFixture fixture) {
    clearMockAi();
    fixture.resetVar(DEFAULT_PROVIDER);
  }

  /** replaces the rules; the first rule matching a request answers it */
  default void mockAi(WebTestMockRule... rules) {
    send("PUT", BodyPublishers.ofString(new ObjectMapper().writeValueAsString(List.of(rules))));
  }

  default void clearMockAi() {
    send("DELETE", BodyPublishers.noBody());
  }

  private static void send(String method, BodyPublisher body) {
    var request = HttpRequest.newBuilder(URI.create(EngineUrl.createRestUrl(WebTestMockRules.PATH)))
        .header("Content-Type", "application/json")
        .header("X-Requested-By", "ivy") // required by the engine for modifying REST calls
        .method(method, body)
        .build();
    try (var client = HttpClient.newHttpClient()) {
      var response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException("Mock AI rules were rejected: " + response.statusCode() + " " + response.body());
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Mock AI rules could not be sent to " + request.uri(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }
}
