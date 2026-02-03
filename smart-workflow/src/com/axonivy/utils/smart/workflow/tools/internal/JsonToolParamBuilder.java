package com.axonivy.utils.smart.workflow.tools.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;

import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.log.Logger;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.internal.JsonSchemaElementUtils.VisitedClassMetadata;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

public class JsonToolParamBuilder {

  private static final Logger LOGGER = Logger.getLogger(JsonToolParamBuilder.class);

  private final Map<Class<?>, VisitedClassMetadata> visited = new HashMap<>();
  private final JsonObjectSchema.Builder builder = JsonObjectSchema.builder();

  public JsonToolParamBuilder() {}

  public JsonObjectSchema toParams(List<StartParameter> variables) {
    variables.stream().forEach(this::toJsonParam);
    return builder.build();
  }

  public void toJsonParam(StartParameter variable) {
    try {
      var type = new QualifiedTypeLoader().load(new QType(variable.typeName()));
      var schema = JsonSchemaElementUtils.jsonSchemaElementFrom(toRawType(type), type, variable.description(), false, visited);
      builder.addProperty(variable.name(), schema);
      return;
    } catch (Exception ex) {
      LOGGER.error("Failed to define json parameter for tool parameter " + variable);
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
