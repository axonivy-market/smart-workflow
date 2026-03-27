package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

class ToolRecorder {

  static Map<String, Object> toolSpecs(List<ToolSpecification> toolSpecifications) {
    if (Objects.isNull(toolSpecifications)) {
      return Map.of();
    }
    var toolSpecs = new LinkedHashMap<String, Object>();
    for (int idx = 0; idx < toolSpecifications.size(); idx++) {
      ToolSpecification toolSpec = toolSpecifications.get(idx);
      try {
        Map<String, Object> functionMap = new LinkedHashMap<>();
        functionMap.put(SemanticConventions.ToolAttributePostfixes.NAME, toolSpec.name());
        if (toolSpec.description() != null) {
          functionMap.put(SemanticConventions.ToolAttributePostfixes.DESCRIPTION, toolSpec.description());
        }
        functionMap.put(
            SemanticConventions.ToolAttributePostfixes.PARAMETERS,
            toolSpec.parameters() != null
                ? OpenInferenceCollector.toolParamMapper.convertValue(toolSpec.parameters(), Map.class)
                : new LinkedHashMap<>());
        Map<String, Object> toolSchemaMap = new LinkedHashMap<>();
        toolSchemaMap.put("type", "function");
        toolSchemaMap.put("function", functionMap);

        toolSpecs.put(
            SemanticConventions.LLM_TOOLS + "." + idx + "."
                + SemanticConventions.TOOL_JSON_SCHEMA,
            OpenInferenceCollector.objectMapper.writeValueAsString(toolSchemaMap));
      } catch (JsonProcessingException ex) {
        Ivy.log().warn("Failed to serialize tool specification at index " + idx, ex);
      }
    }
    return toolSpecs;
  }

  static Map<String, Object> recordToolMessages(String prefix, ChatMessage message) {
    var toolExec = new LinkedHashMap<String, Object>();
    if (message.type().equals(ChatMessageType.AI)) {
      AiMessage aiMessage = (AiMessage) message;
      toolExec.putAll(toolRequest(prefix, aiMessage.toolExecutionRequests()));
    }
    if (message.type().equals(ChatMessageType.TOOL_EXECUTION_RESULT)) {
      ToolExecutionResultMessage toolExecutionResultMessage = (ToolExecutionResultMessage) message;
      toolExec.put(
          prefix + SemanticConventions.MESSAGE_TOOL_CALL_ID,
          toolExecutionResultMessage.id());
    }
    return toolExec;
  }

  static Map<String, Object> toolRequest(String prefix, List<ToolExecutionRequest> toolRequests) {
    if (toolRequests == null) {
      return Map.of();
    }
    var toolAttrs = new LinkedHashMap<String, Object>();
    for (int i = 0; i < toolRequests.size(); i++) {
      toolAttrs.put(
          prefix + SemanticConventions.MESSAGE_TOOL_CALLS + "." + i + "."
              + SemanticConventions.TOOL_CALL_ID,
          toolRequests.get(i).id());
      toolAttrs.put(
          prefix + SemanticConventions.MESSAGE_TOOL_CALLS + "." + i + "."
              + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON,
          toolRequests.get(i).arguments());
      toolAttrs.put(
          prefix + SemanticConventions.MESSAGE_TOOL_CALLS + "." + i + "."
              + SemanticConventions.TOOL_CALL_FUNCTION_NAME,
          toolRequests.get(i).name());
    }
    return toolAttrs;
  }
}
