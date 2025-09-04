package com.axonivy.utils.ai;

import java.time.Duration;

import ch.ivyteam.ivy.jersey.client.JerseyClientBuilder;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;

public class SmartHttpClientBuilder implements HttpClientBuilder {

  private final JerseyClientBuilder jersey;

  public SmartHttpClientBuilder() {
    this.jersey = JerseyClientBuilder.create("smart-orchestrator");
  }

  @Override
  public Duration connectTimeout() {
    return null;
  }

  @Override
  public HttpClientBuilder connectTimeout(Duration timeout) {
    jersey.connectTimeoutInMillis((int) timeout.toMillis());
    return this;
  }

  @Override
  public Duration readTimeout() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HttpClientBuilder readTimeout(Duration timeout) {
    jersey.connectTimeoutInMillis((int) timeout.toMillis());
    return this;
  }

  @Override
  public HttpClient build() {
    return new SmartHttpClient(jersey.toClient());
  }

}
