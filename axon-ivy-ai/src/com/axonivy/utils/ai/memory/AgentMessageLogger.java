package com.axonivy.utils.ai.memory;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.utils.IdGenerationUtils;

import dev.langchain4j.data.message.ChatMessage;

public class AgentMessageLogger {
  private PersistentAgentMemoryStore memoryStore;

  // Execution context
  private String executionId;

  private List<ChatMessage> messages;

  public AgentMessageLogger() {
    this.executionId = IdGenerationUtils.generateRandomId();
    memoryStore = PersistentAgentMemoryStore.getInstance();
    messages = new ArrayList<>();
  }

  public AgentMessageLogger(String executionId) {
    this.executionId = executionId;
    memoryStore = PersistentAgentMemoryStore.getInstance();
    messages = memoryStore.getMessages(executionId);
  }

  /**
   * Main logging method - creates and stores a log entry
   */
  public void log(LogLevel level, LogPhase phase, String content, String executionContext, int iteration) {
    // Add new message to the current message list
    messages.add(AgentMessage.getBuilder()
        .id(Integer.toString(messages.size()))
        .executionId(executionId)
        .level(level)
        .phase(phase)
        .content(content)
        .executionContext(executionContext)
        .iteration(iteration).build());

    // Persist the updated message list
    memoryStore.updateMessages(executionContext, messages);
  }
  
  public void logAdaptivePlan(LogPhase phase, String content, String executionContext, int stepNumber, int iteration,
      String toolId) {
    messages.add(AgentMessage.getBuilder()
        .id(Integer.toString(messages.size()))
        .executionId(executionId)
        .level(LogLevel.PLANNING)
        .phase(phase)
        .content(content)
        .executionContext(executionContext)
        .toolId(toolId)
        .iteration(iteration)
        .stepNumber(stepNumber).build());

    // Persist the updated message list
    memoryStore.updateMessages(executionContext, messages);
  }

  public List<ChatMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<ChatMessage> messages) {
    this.messages = messages;
  }
}
