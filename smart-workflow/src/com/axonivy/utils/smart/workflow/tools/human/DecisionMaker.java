package com.axonivy.utils.smart.workflow.tools.human;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.memory.store.BusinessDataMemory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public class DecisionMaker {

  private final String memoryId;

  public DecisionMaker(String memoryId) {
    this.memoryId = memoryId;
  }

  public void resolve(String decision) { 
    var messages = new ArrayList<>(new BusinessDataMemory().getMessages(memoryId));
    if (messages.isEmpty()) {
      throw new IllegalStateException("Found no pending ChatMemory for id: " + memoryId);
    }
    var invoker = findInvoke(messages);
    if (invoker.isEmpty()) {
      throw new IllegalStateException("Found no pending AiMessage for id: " + memoryId);
    }
    var request = pending(invoker.get().toolExecutionRequests(), messages);
    if (request.isEmpty()) {
      throw new IllegalStateException("Found no pending ToolExecutionRequest for id: " + memoryId);
    }

    var result = ToolExecutionResultMessage.from(request.get(), decision);
    messages.add(result);
    new BusinessDataMemory().updateMessages(memoryId, messages);
  }

  private static Optional<ToolExecutionRequest> pending(List<ToolExecutionRequest> toolExecutionRequests, List<ChatMessage> messages) {
    var results = messages.stream()
        .filter(ToolExecutionResultMessage.class::isInstance)
        .map(ToolExecutionResultMessage.class::cast)
        .map(ToolExecutionResultMessage::id)
        .toList();
    return toolExecutionRequests.stream()
      .filter(r -> !results.contains(r.id()))
      .findFirst();
  }

  private static Optional<AiMessage> findInvoke(List<ChatMessage> messages) {
    return messages.reversed().stream()
        .filter(AiMessage.class::isInstance)
        .map(AiMessage.class::cast)
        .filter(ai -> !ai.toolExecutionRequests().isEmpty())
        .findFirst();
  }

}
