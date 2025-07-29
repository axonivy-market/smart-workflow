package com.axonivy.utils.ai.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.mock.MockOpenAI;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.model.openai.OpenAiChatModel;

@IvyTest
class TestOpenAiServiceConnector {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @Test
  void chatRaw() {
    var openAi = mockClient();
    var response = openAi.chat("ready?");
    assertThat(response).contains("How can I assist you today?");
  }

  @Test
  void requestLogAccess() {
    mockClient().chat("ready?");

    assertThat(httpRequestLog())
        .as("transport logs are easy to access and assert in tests")
        .contains("url: http://")
        .contains("/api/aiMock/chat/completions");
  }

  private String httpRequestLog() {
    return log.infos().stream()
        .filter(line -> line.startsWith("HTTP request"))
        .findFirst()
        .orElseThrow();
  }

  private static OpenAiChatModel mockClient() {
    var localApi = EngineUrl.createRestUrl(MockOpenAI.PATH_SUFFIX);
    return new OpenAiServiceConnector()
        .buildOpenAiModel()
        .baseUrl(localApi)
        .customHeaders(Map.of(CsrfProtectionFilter.HEADER_NAME, "ivy"))
        .build();
  }

}
