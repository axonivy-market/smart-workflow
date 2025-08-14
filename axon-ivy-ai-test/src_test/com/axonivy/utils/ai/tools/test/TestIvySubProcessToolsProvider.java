package com.axonivy.utils.ai.tools.test;

import static ch.ivyteam.test.client.OpenAiTestClient.aiMock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.ai.core.IvySubProcessToolsProvider;
import com.axonivy.utils.ai.core.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.ai.core.internal.IvySubProcessToolSpecs;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;

@IvyProcessTest(enableWebServer = true)
class TestIvySubProcessToolsProvider {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl());
    fixture.var(OpenAiConf.TEST_HEADER, "tool");
  }

  @Test
  void chatWithTools() {
    var model = aiMock();
    UserMessage init = UserMessage.from("Help me, my computer is beeping, it started after opening AxonIvy Portal.");

    List<ToolSpecification> ivyTools = IvySubProcessToolSpecs.find();
    ChatRequest request = ChatRequest.builder()
        .messages(init)
        .toolSpecifications(ivyTools)
        .build();
    ChatResponse response = model.chat(request);
    AiMessage aiMessage = response.aiMessage();

    assertThat(aiMessage.toolExecutionRequests())
        .isNotEmpty();

    var results = aiMessage.toolExecutionRequests().stream()
        .map(IvySubProcessToolExecutor::execute).toList();

    ChatRequest request2 = ChatRequest.builder()
        .messages(Stream.concat(Stream.of((ChatMessage) init, aiMessage), results.stream()).toList())
        .toolSpecifications(ivyTools)
        .build();
    ChatResponse response2 = model.chat(request2);
    assertThat(response2).isNotNull();

    log.debugs().forEach(System.out::println);
  }

  interface SupportAgent {
    String chat(String query);
  }

  @Test
  void chatAgentic() {
    var supporter = AiServices.builder(SupportAgent.class)
        .chatModel(aiMock())
        .toolProvider(new IvySubProcessToolsProvider())
        .build();
    String chat = supporter.chat("Help me, my computer is beeping, it started after opening AxonIvy Portal.");
    System.out.println(chat);
  }

}
