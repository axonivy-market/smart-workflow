package com.axonivy.utils.smart.workflow.model.openai;

import static com.axonivy.utils.smart.workflow.client.OpenAiTestClient.aiMock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;

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
    MockOpenAI.defineResponses(request -> Response.ok().entity("""
        {"id":"resp_test","model":"gpt-6-astra","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"ok"}]}]}
        """).build());
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

  @Test
  void reasoningEffortUsesResponsesApi(AppFixture fixture) {
    AtomicReference<JsonNode> request = new AtomicReference<>();
    MockOpenAI.defineResponses(json -> {
      request.set(json);
      return Response.ok().entity("""
          {"id":"resp_test","model":"gpt-6-astra","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"ok"}]}]}
          """).build();
    });

    ChatModel model = new OpenAiModelProvider().setup(ModelOptions.builder()
        .modelName("gpt-6-astra")
        .reasoningEffort("max")
        .build());

    assertThat(model.chat("hello")).isEqualTo("ok");
    assertThat(request.get().path("reasoning").path("effort").asText()).isEqualTo("max");
    assertThat(httpRequestLog()).contains("/responses");
  }

  private String httpRequestLog() {
    return log.infos().stream()
        .filter(line -> line.startsWith("HTTP request"))
        .findFirst()
        .orElseThrow();
  }

}
