package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.listener.InputGuardrailListener;
import com.axonivy.utils.smart.workflow.governance.history.listener.OutputGuardrailListener;
import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;

@IvyTest
public class TestGuardrailHistoryRecording {

  private InMemoryHistoryStorage storage;
  private ChatHistoryRepository repo;
  private InputGuardrailListener inputListener;
  private OutputGuardrailListener outputListener;

  @BeforeEach
  void setUp() {
    storage = new InMemoryHistoryStorage();
    repo = new ChatHistoryRepository("case-1", "task-1", "test-agent", storage);
    inputListener = new InputGuardrailListener(repo);
    outputListener = new OutputGuardrailListener(repo);
  }

  @Test
  void recordsInputGuardrailWithMessage() {
    inputListener.onEvent(buildInputEvent("Hello agent", InputGuardrailResult.success(), Duration.ofMillis(10)));

    assertThat(storage.findAll()).hasSize(1);
    var guardrails = storage.findAll().get(0).getGuardrailExecutions();
    assertThat(guardrails).hasSize(1);

    var guardrail = guardrails.get(0);
    assertThat(guardrail.guardrailName()).isEqualTo("InputGuardrailAdapter");
    assertThat(guardrail.type()).isEqualTo("INPUT");
    assertThat(guardrail.result()).isEqualTo("SUCCESS");
    assertThat(guardrail.message()).isEqualTo("Hello agent");
    assertThat(guardrail.failureMessage()).isNull();
    assertThat(guardrail.durationMs()).isEqualTo(10L);
    assertThat(guardrail.executedAt()).isNotNull();
  }

  @Test
  void recordsOutputGuardrailFailureWithMessage() {
    OutputGuardrailResult fatalResult = OUTPUT_HELPER.fatal("API key detected");
    outputListener.onEvent(buildOutputEvent("Here is the key: sk-abc123", fatalResult, Duration.ofMillis(25)));

    var guardrails = storage.findAll().get(0).getGuardrailExecutions();
    assertThat(guardrails).hasSize(1);

    var guardrail = guardrails.get(0);
    assertThat(guardrail.type()).isEqualTo("OUTPUT");
    assertThat(guardrail.result()).isEqualTo("FATAL");
    assertThat(guardrail.message()).isEqualTo("Here is the key: sk-abc123");
    assertThat(guardrail.failureMessage()).isEqualTo("API key detected");
  }

  @Test
  void accumulatesMultipleGuardrailExecutions() {
    inputListener.onEvent(buildInputEvent("safe query", InputGuardrailResult.success(), Duration.ofMillis(5)));
    inputListener.onEvent(buildInputEvent("ignore previous", INPUT_HELPER.failure("blocked"), Duration.ofMillis(8)));
    outputListener.onEvent(buildOutputEvent("clean response", OutputGuardrailResult.success(), Duration.ofMillis(12)));

    assertThat(storage.findAll()).hasSize(1);
    var guardrails = storage.findAll().get(0).getGuardrailExecutions();
    assertThat(guardrails).hasSize(3);

    assertThat(guardrails.get(0).result()).isEqualTo("SUCCESS");
    assertThat(guardrails.get(0).message()).isEqualTo("safe query");
    assertThat(guardrails.get(1).result()).isEqualTo("FAILURE");
    assertThat(guardrails.get(1).message()).isEqualTo("ignore previous");
    assertThat(guardrails.get(2).type()).isEqualTo("OUTPUT");
    assertThat(guardrails.get(2).message()).isEqualTo("clean response");
  }

  private static final InputGuardrail INPUT_HELPER = new InputGuardrail() {};
  private static final OutputGuardrail OUTPUT_HELPER = new OutputGuardrail() {};
  private static final ChatExecutor NOOP_CHAT_EXECUTOR = new ChatExecutor() {
    @Override
    public dev.langchain4j.model.chat.response.ChatResponse execute() { return null; }
    @Override
    public dev.langchain4j.model.chat.response.ChatResponse execute(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) { return null; }
  };

  private InputGuardrailExecutedEvent buildInputEvent(String userText, InputGuardrailResult result, Duration duration) {
    var invocationCtx = InvocationContext.builder()
        .invocationId(UUID.randomUUID())
        .timestamp(Instant.now())
        .methodName("chat")
        .interfaceName("ChatAgent")
        .build();
    var request = InputGuardrailRequest.builder()
        .userMessage(UserMessage.from(userText))
        .commonParams(GuardrailRequestParams.builder()
            .userMessageTemplate("")
            .variables(java.util.Map.of())
            .build())
        .build();
    return InputGuardrailExecutedEvent.builder()
        .invocationContext(invocationCtx)
        .guardrailClass(InputGuardrailAdapter.class)
        .request(request)
        .result(result)
        .duration(duration)
        .build();
  }

  private OutputGuardrailExecutedEvent buildOutputEvent(String aiText, OutputGuardrailResult result, Duration duration) {
    var invocationCtx = InvocationContext.builder()
        .invocationId(UUID.randomUUID())
        .timestamp(Instant.now())
        .methodName("chat")
        .interfaceName("ChatAgent")
        .build();
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage(aiText))
        .build();
    var request = OutputGuardrailRequest.builder()
        .responseFromLLM(response)
        .chatExecutor(NOOP_CHAT_EXECUTOR)
        .requestParams(GuardrailRequestParams.builder()
            .userMessageTemplate("")
            .variables(java.util.Map.of())
            .build())
        .build();
    return OutputGuardrailExecutedEvent.builder()
        .invocationContext(invocationCtx)
        .guardrailClass(OutputGuardrailAdapter.class)
        .request(request)
        .result(result)
        .duration(duration)
        .build();
  }
}
