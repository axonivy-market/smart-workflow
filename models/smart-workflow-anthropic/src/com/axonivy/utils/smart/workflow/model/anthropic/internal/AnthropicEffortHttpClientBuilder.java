package com.axonivy.utils.smart.workflow.model.anthropic.internal;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import tools.jackson.core.type.TypeReference;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

final class AnthropicEffortHttpClientBuilder implements HttpClientBuilder {

  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {
  };

  private final HttpClientBuilder delegate;
  private final String effort;

  AnthropicEffortHttpClientBuilder(HttpClientBuilder delegate, String effort) {
    this.delegate = delegate;
    this.effort = effort;
  }

  static String withEffort(String requestBody, String effort) {
    Map<String, Object> body = JsonUtils.getObjectMapper().readValue(requestBody, JSON_OBJECT);
    Object outputConfig = body.get("output_config");
    Map<String, Object> config = new LinkedHashMap<>();
    if (outputConfig instanceof Map<?, ?> existingConfig) {
      existingConfig.forEach((key, value) -> {
        if (!(key instanceof String stringKey)) {
          throw new IllegalStateException("Anthropic output_config contains a non-string key.");
        }
        config.put(stringKey, value);
      });
    } else if (outputConfig != null) {
      throw new IllegalStateException("Anthropic output_config must be a JSON object.");
    }
    config.put("effort", effort);
    body.put("output_config", config);
    return JsonUtils.getObjectMapper().writeValueAsString(body);
  }

  @Override
  public Duration connectTimeout() {
    return delegate.connectTimeout();
  }

  @Override
  public HttpClientBuilder connectTimeout(Duration timeout) {
    delegate.connectTimeout(timeout);
    return this;
  }

  @Override
  public Duration readTimeout() {
    return delegate.readTimeout();
  }

  @Override
  public HttpClientBuilder readTimeout(Duration timeout) {
    delegate.readTimeout(timeout);
    return this;
  }

  @Override
  public HttpClient build() {
    HttpClient client = delegate.build();
    return new HttpClient() {
      @Override
      public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
        return client.execute(withEffort(request));
      }

      @Override
      public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        client.execute(withEffort(request), parser, listener);
      }
    };
  }

  private HttpRequest withEffort(HttpRequest request) {
    if (request.body() == null || !URI.create(request.url()).getPath().endsWith("/messages")) {
      return request;
    }
    return HttpRequest.builder()
        .method(request.method())
        .url(request.url())
        .headers(request.headers())
        .body(withEffort(request.body(), effort))
        .build();
  }
}
