package com.axonivy.utils.smart.workflow.governance.history.recorder;

public interface GuardrailExecutionRecorder {

  void recordGuardrail(String guardrailName, String type, String result, String message, String failureMessage, Long durationMs);
}
