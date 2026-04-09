package com.axonivy.utils.smart.workflow.tools.adapter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader;
import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.provider.ToolParameter;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.internal.JsonSchemaElementUtils.VisitedClassMetadata;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
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

  private JsonSchemaElement toJsonSchema(ToolParameter p) {
    try {
      Type type = new QualifiedTypeLoader(tool.getClass().getClassLoader()).load(new QType(p.type()));
      Map<Class<?>, VisitedClassMetadata> visited = new HashMap<>();
      return JsonSchemaElementUtils.jsonSchemaElementFrom(toRawType(type), type, p.description(), false, visited);
    } catch (Exception ex) {
      Ivy.log().warn("Failed to define schema for parameter '" + p.name() + "': " + ex.getMessage());
      return JsonStringSchema.builder().description(p.description()).build();
    }
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
        Type type = new QualifiedTypeLoader(tool.getClass().getClassLoader()).load(new QType(p.type()));
        result.put(p.name(), MAPPER.reader().forType(type).readValue(node));
      }
    } catch (IOException | ClassNotFoundException ex) {
      Ivy.log().warn("Failed to parse tool arguments for '" + tool.name() + "': " + ex.getMessage());
    }
    return result;
  }

  private static Class<?> toRawType(Type type) {
    if (type instanceof ParameterizedType pt) {
      return (Class<?>) pt.getRawType();
    }
    return (Class<?>) type;
  }
}
