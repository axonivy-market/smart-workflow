package com.axonivy.utils.smart.orchestrator.client;

import dev.langchain4j.http.client.HttpClientBuilder;

public class SmartHttpClientBuilderFactory implements dev.langchain4j.http.client.HttpClientBuilderFactory {

  @Override
  public HttpClientBuilder create() {
    return new SmartHttpClientBuilder();
  }

}
