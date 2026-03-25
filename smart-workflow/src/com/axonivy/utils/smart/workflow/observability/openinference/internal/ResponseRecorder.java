package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

class ResponseRecorder {

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  public Map<String, Object> handleResponse(ChatResponse response, boolean hideOutput) {
    if (response.finishReason() != null) {
      attributes.put(
          "llm.response.finish_reasons",
          List.of(response.finishReason().name()));
    }

    if (!hideOutput && response.aiMessage() != null) {
      outputMessages(response);
    }

    tokenUsage(response);
    return attributes;
  }

  private void tokenUsage(ChatResponse response) {
    if (response.tokenUsage() == null) {
      return;
    }
    attributes.put(SemanticConventions.LLM_TOKEN_COUNT_PROMPT,
        (long) response.tokenUsage().inputTokenCount());
    attributes.put(SemanticConventions.LLM_TOKEN_COUNT_COMPLETION,
        (long) response.tokenUsage().outputTokenCount());
    attributes.put(SemanticConventions.LLM_TOKEN_COUNT_TOTAL,
        (long) response.tokenUsage().totalTokenCount());
  }

  private void outputMessages(ChatResponse response) {
    AiMessage aiMessage = response.aiMessage();
    String prefix = String.format("%s.%d.", SemanticConventions.LLM_OUTPUT_MESSAGES, 0);
    attributes.put(prefix + SemanticConventions.MESSAGE_ROLE, "assistant");
    attributes.put(prefix + SemanticConventions.MESSAGE_CONTENT, aiMessage.text());
    attributes.putAll(ToolRecorder.toolRequest(prefix, aiMessage.toolExecutionRequests()));
    attributes.put(SemanticConventions.OUTPUT_VALUE, aiMessage.text());
    attributes.put(SemanticConventions.OUTPUT_MIME_TYPE, "text/plain");
  }
}
