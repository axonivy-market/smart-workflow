package com.axonivy.utils.ai;

import dev.langchain4j.http.client.HttpClientBuilder;

public class SmartHttpClientBuilderFactory implements dev.langchain4j.http.client.HttpClientBuilderFactory {

  @Override
  public HttpClientBuilder create() {
    return new SmartHttpClientBuilder();
  }

}
