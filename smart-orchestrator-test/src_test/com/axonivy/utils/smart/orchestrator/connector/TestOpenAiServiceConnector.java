package com.axonivy.utils.smart.orchestrator.connector;

import static com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient.aiMock;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@RestResourceTest
class TestOpenAiServiceConnector {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("chat"));
    MockOpenAI.defineChat(request -> {
      if (request.toPrettyString().contains("ready?")) {
        return responder.send("completions-response.json");
      }
      return Response.serverError().build();
    });
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
