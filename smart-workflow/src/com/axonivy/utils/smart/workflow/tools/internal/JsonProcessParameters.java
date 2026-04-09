package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool.ToolParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonProcessParameters {

  private static final Logger LOGGER = Logger.getLogger(JsonProcessParameters.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ClassLoader classLoader;

  public JsonProcessParameters() {
    this(null);
  }

  public JsonProcessParameters(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Map<String, Object> readParams(List<ToolParameter> parameters, String rawJsonArgs) {
    try {
      if (CollectionUtils.isEmpty(parameters)) {
        return Map.of();
      }
      return toParams(parameters, MAPPER.readTree(rawJsonArgs));
    } catch (JsonProcessingException ex) {
      LOGGER.error("Failed to create parameters from " + rawJsonArgs, ex);
      return Map.of();
    }
  }

  public Map<String, Object> toParams(List<ToolParameter> parameters, JsonNode rawArgs) {
    var map = new LinkedHashMap<String, Object>();
    parameters.forEach(p -> map.put(p.name(), toValue(p, rawArgs)));
    return map;
  }

  private Object toValue(ToolParameter parameter, JsonNode rawArgs) {
    try {
      var typed = new QualifiedTypeLoader(classLoader).load(new QType(parameter.type()));
      var jArg = rawArgs.get(parameter.name());
      if (jArg == null) {
        return null;
      }
      return MAPPER.reader().forType(typed).readValue(jArg);
    } catch (Exception ex) {
      LOGGER.error("Failed to load value of variable " + parameter, ex);
      return null;
    }
  }

}
