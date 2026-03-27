package com.axonivy.utils.smart.workflow.governance.history.listener;

import com.axonivy.utils.smart.workflow.governance.history.recorder.ToolExecutionRecorder;

import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public class ToolExecutionListener implements AiServiceListener<ToolExecutedEvent> {

  private final ToolExecutionRecorder recorder;

  public ToolExecutionListener(ToolExecutionRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public Class<ToolExecutedEvent> getEventClass() {
    return ToolExecutedEvent.class;
  }

  @Override
  public void onEvent(ToolExecutedEvent event) {
    recorder.record(
        event.request().name(),
        event.request().arguments(),
        event.resultText());
  }
}
