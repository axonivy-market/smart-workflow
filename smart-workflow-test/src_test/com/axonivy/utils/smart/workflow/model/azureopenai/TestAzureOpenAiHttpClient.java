package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.client.SmartAzureHttpClientProvider;

import com.azure.core.util.HttpClientOptions;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAzureOpenAiHttpClient {

  @Test
  void smartIvyRestClient_isDefault() {
    var builder = AzureOpenAiServiceConnector.buildOpenAiModel(null);
    assertThat(builder).extracting("httpClientProvider")
        .as("use ivy-jersey client; which integrates into our tracing tools")
        .isInstanceOf(SmartAzureHttpClientProvider.class);
  }

  @Test
  void timeouts_areWiredOntoTheTarget() {
    var options = new HttpClientOptions()
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(7));

    assertThat(new SmartAzureHttpClientProvider().createInstance(options))
        .extracting("target")
        .isInstanceOfSatisfying(WebTarget.class, target -> {
          var config = target.getConfiguration();
          assertThat(config.getProperty("jersey.config.client.connectTimeout")).isEqualTo(5000L);
          assertThat(config.getProperty("jersey.config.client.readTimeout")).isEqualTo(7000L);
        });
  }

}
