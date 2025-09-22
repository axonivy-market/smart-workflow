package com.axonivy.utils.smart.workflow.client;

import java.util.Map;

import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class OpenAiTestClient {

  public static String localMockApiUrl(String test) {
    return Ivy.rest().client("mockClient").getUri().toASCIIString() + "/" + test;
  }

  public static OpenAiChatModel aiMock() {
    return OpenAiServiceConnector
        .buildOpenAiModel()
        .customHeaders(Map.of("X-Requested-By", "ivy"))
        .build();
  }

}
