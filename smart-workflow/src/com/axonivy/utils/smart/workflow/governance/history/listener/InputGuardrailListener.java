package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.util.Optional;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.recorder.GuardrailExecutionRecorder;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector;
import com.axonivy.utils.smart.workflow.guardrails.input.PiiMaskingInputGuardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailResult.Failure;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.listener.InputGuardrailExecutedListener;

public class InputGuardrailListener implements InputGuardrailExecutedListener {

  private final GuardrailExecutionRecorder recorder;

  public InputGuardrailListener(GuardrailExecutionRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public void onEvent(InputGuardrailExecutedEvent event) {
    String guardrailName = event.guardrailClass().getSimpleName();
    String result = event.result().result().name();
    String rawMessage = Optional.ofNullable(event.request())
        .map(r -> r.userMessage())
        .map(UserMessage::singleText)
        .orElse(null);
    // Record the masked form so raw PII is never stored in audit history
    String message = PiiMaskingInputGuardrail.class.getSimpleName().equals(guardrailName) && rawMessage != null
        ? PiiDetector.detectAndMask(rawMessage).maskedText()
        : rawMessage;
    String failureMessage = event.result().failures().stream()
        .map(Failure::message)
        .filter(m -> m != null && !m.isBlank())
        .collect(Collectors.joining("; "));
    long durationMs = event.duration().toMillis();
    recorder.recordGuardrail(guardrailName, "INPUT", result, message,
        failureMessage.isEmpty() ? null : failureMessage, durationMs);
  }
}
