package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.listener.OutputGuardrailListener;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;

public class TestOutputGuardrailListener {

  private List<CapturedGuardrail> captured;
  private OutputGuardrailListener listener;

  record CapturedGuardrail(String name, String type, String result, String message, String failureMessage, Long durationMs) {}

  @BeforeEach
  void setUp() {
    captured = new ArrayList<>();
    listener = new OutputGuardrailListener(
        (name, type, result, message, failureMessage, durationMs) ->
            captured.add(new CapturedGuardrail(name, type, result, message, failureMessage, durationMs)));
  }

  @Test
  void recordsSuccessfulOutputGuardrail() {
    listener.onEvent(buildEvent("The weather is sunny today.", OutputGuardrailResult.success(), Duration.ofMillis(20)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.name()).isEqualTo("OutputGuardrailAdapter");
    assertThat(entry.type()).isEqualTo("OUTPUT");
    assertThat(entry.result()).isEqualTo("SUCCESS");
    assertThat(entry.message()).isEqualTo("The weather is sunny today.");
    assertThat(entry.failureMessage()).isNull();
    assertThat(entry.durationMs()).isEqualTo(20L);
  }

  @Test
  void recordsFatalOutputGuardrailWithMessage() {
    OutputGuardrailResult fatalResult = GUARDRAIL_HELPER.fatal("Sensitive data detected");

    listener.onEvent(buildEvent("Here is the API key: sk-abc123", fatalResult, Duration.ofMillis(33)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.type()).isEqualTo("OUTPUT");
    assertThat(entry.result()).isEqualTo("FATAL");
    assertThat(entry.message()).isEqualTo("Here is the API key: sk-abc123");
    assertThat(entry.failureMessage()).isEqualTo("Sensitive data detected");
    assertThat(entry.durationMs()).isEqualTo(33L);
  }

  @Test
  void recordsFailedOutputGuardrail() {
    OutputGuardrailResult failureResult = GUARDRAIL_HELPER.failure("Content policy violation");

    listener.onEvent(buildEvent("bad content", failureResult, Duration.ofMillis(10)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.result()).isEqualTo("FAILURE");
    assertThat(entry.message()).isEqualTo("bad content");
    assertThat(entry.failureMessage()).isEqualTo("Content policy violation");
  }

  private static final OutputGuardrail GUARDRAIL_HELPER = new OutputGuardrail() {};
  private static final ChatExecutor NOOP_CHAT_EXECUTOR = new ChatExecutor() {
    @Override
    public dev.langchain4j.model.chat.response.ChatResponse execute() { return null; }
    @Override
    public dev.langchain4j.model.chat.response.ChatResponse execute(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) { return null; }
  };

  private OutputGuardrailExecutedEvent buildEvent(String aiText, OutputGuardrailResult result, Duration duration) {
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
