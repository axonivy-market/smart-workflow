package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.util.Optional;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.recorder.GuardrailExecutionRecorder;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.GuardrailResult.Failure;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.listener.OutputGuardrailExecutedListener;

public class OutputGuardrailListener implements OutputGuardrailExecutedListener {

  private final GuardrailExecutionRecorder recorder;

  public OutputGuardrailListener(GuardrailExecutionRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public void onEvent(OutputGuardrailExecutedEvent event) {
    String guardrailName = event.guardrailClass().getSimpleName();
    String result = event.result().result().name();
    String message = Optional.ofNullable(event.request())
        .map(r -> r.responseFromLLM())
        .map(r -> r.aiMessage())
        .map(AiMessage::text)
        .orElse(null);
    String failureMessage = event.result().failures().stream()
        .map(Failure::message)
        .filter(m -> m != null && !m.isBlank())
        .collect(Collectors.joining("; "));
    long durationMs = event.duration().toMillis();
    recorder.recordGuardrail(guardrailName, "OUTPUT", result, message,
        failureMessage.isEmpty() ? null : failureMessage, durationMs);
  }
}
