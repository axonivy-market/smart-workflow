package com.axonivy.utils.smart.orchestrator.connector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SmartHttpClientBuilder;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestOpenAiHttpClient {

  @Test
  void smartJerseyIsDefault() {
    var builder = OpenAiServiceConnector.buildOpenAiModel();
    assertThat(builder).extracting("httpClientBuilder")
        .as("use ivy-jersey client; which integrates into our tracing tools")
        .isInstanceOf(SmartHttpClientBuilder.class);
  }

}
