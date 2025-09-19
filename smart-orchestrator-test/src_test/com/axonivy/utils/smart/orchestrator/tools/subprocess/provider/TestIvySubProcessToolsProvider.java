package com.axonivy.utils.smart.orchestrator.tools.subprocess.provider;

import static com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient.aiMock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.orchestrator.client.OpenAiTestClient;
import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.orchestrator.tools.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.orchestrator.tools.internal.IvySubProcessToolExecutor;
import com.axonivy.utils.smart.orchestrator.tools.internal.IvySubProcessToolSpecs;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;

@RestResourceTest
class TestIvySubProcessToolsProvider {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(SubProcessToolsProviderChat::toolTest);
  }

  @Test
  void chatWithTools() {
    var model = aiMock();
    UserMessage init = UserMessage.from("Who am I?");

    List<ToolSpecification> ivyTools = IvySubProcessToolSpecs.find().stream()
        .filter(spec -> "whoami".equals(spec.name())).toList();
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
    List<String> selectedTools = new ArrayList<>();
    selectedTools.add("createSupportTicket");
    var supporter = AiServices.builder(SupportAgent.class)
        .chatModel(aiMock())
        .toolProvider(new IvySubProcessToolsProvider().filtering(selectedTools))
        .build();
    String chat = supporter.chat("Help me, my computer is beeping, it started after opening AxonIvy Portal.");
    System.out.println(chat);
    assertThat(!chat.isEmpty());
  }

}
