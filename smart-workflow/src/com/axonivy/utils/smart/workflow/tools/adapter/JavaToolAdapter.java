package com.axonivy.utils.smart.workflow.tools.adapter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.entity.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.entity.ToolParameter;
import com.axonivy.utils.smart.workflow.tools.entity.ToolParameter.ParameterType;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.internal.JsonSchemaElementUtils.VisitedClassMetadata;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.tool.ToolExecutor;

public class JavaToolAdapter {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SmartWorkflowTool tool;

  public JavaToolAdapter(SmartWorkflowTool tool) {
    this.tool = tool;
  }

  public ToolSpecification toToolSpecification() {
    var builder = ToolSpecification.builder()
        .name(tool.name())
        .description(tool.description());
    if (!tool.parameters().isEmpty()) {
      var schemaBuilder = JsonObjectSchema.builder();
      var requiredNames = new java.util.ArrayList<String>();
      for (ToolParameter p : tool.parameters()) {
        schemaBuilder.addProperty(p.name(), toJsonSchema(p));
        requiredNames.add(p.name());
      }
      builder.parameters(schemaBuilder.required(requiredNames).build());
    }
    return builder.build();
  }

  public ToolExecutor toToolExecutor() {
    return (request, memoryId) -> {
      Map<String, Object> args = parseArgs(request.arguments());
      Object result = tool.execute(args);
      if (result instanceof String s) {
        return s;
      }
      return Json.toJson(result);
    };
  }

  private static JsonSchemaElement toJsonSchema(ToolParameter p) {
    Class<?> javaClass = toJavaClass(p.type());
    Map<Class<?>, VisitedClassMetadata> visited = new HashMap<>();
    return JsonSchemaElementUtils.jsonSchemaElementFrom(javaClass, javaClass, p.description(), false, visited);
  }

  private static Class<?> toJavaClass(ParameterType type) {
    return switch (type) {
      case STRING -> String.class;
      case NUMBER -> Double.class;
      case INTEGER -> Long.class;
      case BOOLEAN -> Boolean.class;
      case STRING_ARRAY -> String[].class;
      case NUMBER_ARRAY -> Double[].class;
    };
  }

  private Map<String, Object> parseArgs(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    try {
      var root = MAPPER.readTree(rawJson);
      for (ToolParameter p : tool.parameters()) {
        var node = root.get(p.name());
        if (node == null || node.isNull()) {
          result.put(p.name(), null);
          continue;
        }
        result.put(p.name(), switch (p.type()) {
          case STRING -> node.asText();
          case NUMBER -> node.asDouble();
          case INTEGER -> node.asLong();
          case BOOLEAN -> node.asBoolean();
          case STRING_ARRAY -> MAPPER.readerForListOf(String.class).readValue(node);
          case NUMBER_ARRAY -> MAPPER.readerForListOf(Double.class).readValue(node);
        });
      }
    } catch (Exception ex) {
      // return partial map; tool handles nulls
    }
    return result;
  }
}
