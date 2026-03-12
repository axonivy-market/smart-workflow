package com.axonivy.utils.smart.workflow.governance.listener;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryRepository;

import dev.langchain4j.data.message.ChatMessage;

public class ChatHistoryRecordingListener extends AbstractChatModelListener {

  private final ChatHistoryRepository recorder;

  public ChatHistoryRecordingListener() {
    this.recorder = null;
  }

  public ChatHistoryRecordingListener(String caseUuid, String taskUuid) {
    this.recorder = new ChatHistoryRepository(this, caseUuid, taskUuid);
  }

  @Override
  protected void record(List<ChatMessage> messages) {
    if (recorder != null) {
      recorder.updateMessages(messages);
    }
  }
}
