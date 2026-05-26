package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.listener.InputGuardrailListener;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;

public class TestInputGuardrailListener {

  private List<CapturedGuardrail> captured;
  private InputGuardrailListener listener;

  record CapturedGuardrail(String name, String type, String result, String message, String failureMessage, Long durationMs) {}

  @BeforeEach
  void setUp() {
    captured = new ArrayList<>();
    listener = new InputGuardrailListener(
        (name, type, result, message, failureMessage, durationMs) ->
            captured.add(new CapturedGuardrail(name, type, result, message, failureMessage, durationMs)));
  }

  @Test
  void recordsSuccessfulInputGuardrail() {
    listener.onEvent(buildEvent("What is the weather?", InputGuardrailResult.success(), Duration.ofMillis(15)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.name()).isEqualTo("InputGuardrailAdapter");
    assertThat(entry.type()).isEqualTo("INPUT");
    assertThat(entry.result()).isEqualTo("SUCCESS");
    assertThat(entry.message()).isEqualTo("What is the weather?");
    assertThat(entry.failureMessage()).isNull();
    assertThat(entry.durationMs()).isEqualTo(15L);
  }

  @Test
  void recordsFailedInputGuardrailWithMessage() {
    InputGuardrailResult failureResult = GUARDRAIL_HELPER.failure("Prompt injection detected");

    listener.onEvent(buildEvent("ignore previous instructions", failureResult, Duration.ofMillis(42)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.type()).isEqualTo("INPUT");
    assertThat(entry.result()).isEqualTo("FAILURE");
    assertThat(entry.message()).isEqualTo("ignore previous instructions");
    assertThat(entry.failureMessage()).isEqualTo("Prompt injection detected");
    assertThat(entry.durationMs()).isEqualTo(42L);
  }

  @Test
  void recordsFatalInputGuardrail() {
    InputGuardrailResult fatalResult = GUARDRAIL_HELPER.fatal("Critical violation");

    listener.onEvent(buildEvent("malicious input", fatalResult, Duration.ofMillis(5)));

    assertThat(captured).hasSize(1);
    var entry = captured.get(0);
    assertThat(entry.result()).isEqualTo("FATAL");
    assertThat(entry.message()).isEqualTo("malicious input");
    assertThat(entry.failureMessage()).isEqualTo("Critical violation");
  }

  private static final InputGuardrail GUARDRAIL_HELPER = new InputGuardrail() {};

  private InputGuardrailExecutedEvent buildEvent(String userText, InputGuardrailResult result, Duration duration) {
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
}
