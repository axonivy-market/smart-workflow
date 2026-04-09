package com.axonivy.utils.smart.workflow.tools.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool.ToolParameter;

import ch.ivyteam.log.Logger;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.internal.JsonSchemaElementUtils.VisitedClassMetadata;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

public class JsonToolParamBuilder {

  private static final Logger LOGGER = Logger.getLogger(JsonToolParamBuilder.class);

  private final ClassLoader classLoader;
  private final Map<Class<?>, VisitedClassMetadata> visited = new HashMap<>();
  private final JsonObjectSchema.Builder builder = JsonObjectSchema.builder();

  public JsonToolParamBuilder() {
    this(null);
  }

  public JsonToolParamBuilder(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public JsonObjectSchema toParams(List<ToolParameter> parameters) {
    if (parameters.isEmpty()) {
      return null; // less tokens + better compliance with Arize Phoenix playground
    }
    var required = new ArrayList<String>();
    parameters.forEach(p -> {
      toJsonParam(p);
      required.add(p.name());
    });
    return builder.required(required).build();
  }

  public void toJsonParam(ToolParameter parameter) {
    try {
      var type = new QualifiedTypeLoader(classLoader).load(new QType(parameter.type()));
      var schema = JsonSchemaElementUtils.jsonSchemaElementFrom(toRawType(type), type, parameter.description(), false, visited);
      builder.addProperty(parameter.name(), schema);
    } catch (Exception ex) {
      LOGGER.error("Failed to define json parameter for tool parameter " + parameter);
      builder.additionalProperties(true); // hint: more parameters which we can't describe
    }
  }

  private static Class<?> toRawType(Type type) {
    if (type instanceof ParameterizedType pt) {
      return (Class<?>) pt.getRawType();
    }
    return (Class<?>) type;
  }
}
