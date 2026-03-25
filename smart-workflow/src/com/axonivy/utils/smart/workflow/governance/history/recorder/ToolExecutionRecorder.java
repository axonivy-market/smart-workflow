package com.axonivy.utils.smart.workflow.governance.history.recorder;

public interface ToolExecutionRecorder {

  void record(String toolName, String arguments, String resultText);
}
