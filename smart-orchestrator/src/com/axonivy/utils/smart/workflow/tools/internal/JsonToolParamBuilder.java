package com.axonivy.utils.smart.workflow.tools.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.scripting.dataclass.IProjectDataClassManager;
import ch.ivyteam.ivy.scripting.system.IIvyScriptClassRepository;
import ch.ivyteam.ivy.scripting.types.QualifiedTypeLoader;
import ch.ivyteam.ivy.scripting.types.QualifiedTypeLoader.QType;
import ch.ivyteam.log.Logger;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.internal.JsonSchemaElementUtils.VisitedClassMetadata;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

@SuppressWarnings("restriction")
public class JsonToolParamBuilder {

  private static final Logger LOGGER = Logger.getLogger(JsonToolParamBuilder.class);

  private final IIvyScriptClassRepository repo;
  private final Map<Class<?>, VisitedClassMetadata> visited = new HashMap<>();
  private final JsonObjectSchema.Builder builder = JsonObjectSchema.builder();

  public JsonToolParamBuilder(IProcessModelVersion pmv) {
    this.repo = IProjectDataClassManager.of(pmv).getIvyScriptClassRepository();
  }

  public JsonObjectSchema toParams(List<VariableDesc> variables) {
    variables.stream().forEach(this::toJsonParam);
    return builder.build();
  }

  public void toJsonParam(VariableDesc variable) {
    try {
      var type = new QualifiedTypeLoader(repo).load(new QType(variable.getType().getName()));
      var schema = JsonSchemaElementUtils.jsonSchemaElementFrom(toRawType(type), type, variable.getInfo().getDescription(), false, visited);
      builder.addProperty(variable.getName(), schema);
      return;
    } catch (ClassNotFoundException | IllegalStateException ex) {
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
