package com.axonivy.utils.smart.workflow.governance.listener;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.memory.SmartWorkflowChatMemoryStore;

import dev.langchain4j.data.message.ChatMessage;

public class SmartWorkflowChatModelListener extends AbstractChatModelListener {

  private final SmartWorkflowChatMemoryStore recorder;

  public SmartWorkflowChatModelListener() {
    this.recorder = null;
  }

  public SmartWorkflowChatModelListener(String caseUuid, String taskUuid) {
    this.recorder = new SmartWorkflowChatMemoryStore(this, caseUuid, taskUuid);
  }

  @Override
  protected void record(List<ChatMessage> messages) {
    if (recorder != null) {
      recorder.updateMessages(null, messages);
    }
  }
}
