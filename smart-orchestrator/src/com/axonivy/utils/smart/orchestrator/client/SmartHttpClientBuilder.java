package com.axonivy.utils.smart.orchestrator.client;

import java.time.Duration;
import java.util.UUID;

import javax.ws.rs.client.WebTarget;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;

public class SmartHttpClientBuilder implements HttpClientBuilder {

  private static final UUID LANG_CHAIN_CLIENT = UUID.fromString("4fc2b880-8b39-4219-baa5-ada7f40ddb92");

  private final WebTarget client;

  public SmartHttpClientBuilder() {
    this.client = Ivy.rest().client(LANG_CHAIN_CLIENT);
  }

  @Override
  public Duration connectTimeout() {
    return null;
  }

  @Override
  public HttpClientBuilder connectTimeout(Duration timeout) {
    client.property("jersey.config.client.connectTimeout", timeout.toMillis());
    return this;
  }

  @Override
  public Duration readTimeout() {
    return null;
  }

  @Override
  public HttpClientBuilder readTimeout(Duration timeout) {
    client.property("jersey.config.client.readTimeout", timeout.toMillis());
    return this;
  }

  @Override
  public HttpClient build() {
    return new SmartHttpClient(client);
  }

}
