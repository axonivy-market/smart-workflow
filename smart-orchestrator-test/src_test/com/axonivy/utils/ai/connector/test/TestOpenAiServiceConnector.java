package com.axonivy.utils.ai.connector.test;

import static ch.ivyteam.test.client.OpenAiTestClient.aiMock;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.ai.mock.MockOpenAI;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyTest(enableWebServer = true)
class TestOpenAiServiceConnector {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("chat"));
    MockOpenAI.defineChat(this::chat);
  }

  private Response chat(JsonNode request) {
    if (request.toPrettyString().contains("ready?")) {
      return Response.ok()
          .entity(TestOpenAiServiceConnector.class.getResourceAsStream("completions-response.json"))
          .build();
    }
    return Response.serverError().build();
  }

  @Test
  void chatRaw() {
    var openAi = aiMock();
    var response = openAi.chat("ready?");
    assertThat(response).contains("How can I assist you today?");
  }

  @Test
  void requestLogAccess() {
    aiMock().chat("ready?");

    assertThat(httpRequestLog())
        .as("transport logs are easy to access and assert in tests")
        .contains("url: http://")
        .contains("/api/aiMock/")
        .contains("/chat/completions");
  }

  private String httpRequestLog() {
    return log.infos().stream()
        .filter(line -> line.startsWith("HTTP request"))
        .findFirst()
        .orElseThrow();
  }

}
