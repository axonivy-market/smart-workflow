package com.axonivy.utils.ai.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.mock.MockOpenAI;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.openai.OpenAiChatModel;

@IvyTest
class TestOpenAiServiceConnector {

  @Test
  void chatRaw() {
    var openAi = mockClient();
    var response = openAi.chat("ready?");
    assertThat(response).contains("How can I assist you today?");
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
