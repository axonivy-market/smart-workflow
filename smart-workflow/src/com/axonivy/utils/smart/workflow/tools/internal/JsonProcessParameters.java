package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.process.call.StartParameter;

@SuppressWarnings("restriction")
public class JsonProcessParameters {

  private static final Logger LOGGER = Logger.getLogger(JsonProcessParameters.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public JsonProcessParameters() {}

  public Map<String, Object> readParams(List<StartParameter> ins, String rawJsonArgs) {
    try {
      if (ins.isEmpty()) {
        return Map.of();
      }
      return toParams(ins, MAPPER.readTree(rawJsonArgs));
    } catch (JsonProcessingException ex) {
      LOGGER.error("Failed to create parameters from " + rawJsonArgs);
      return Map.of();
    }
  }

  public Map<String, Object> toParams(List<StartParameter> ins, JsonNode rawArgs) {
    var map = new LinkedHashMap<String, Object>();
    ins.stream().forEachOrdered(in -> {
      map.put(in.name(), toValue(in, rawArgs));
    });
    return map;
  }

  private Object toValue(StartParameter in, JsonNode rawArgs) {
    try {
      var typed = new QualifiedTypeLoader().load(new QType(in.typeName()));
      var jArg = rawArgs.get(in.name());
      if (jArg == null) {
        return null;
      }
      return MAPPER.reader().forType(typed).readValue(jArg);
    } catch (Exception ex) {
      LOGGER.error("Failed to load value of variable " + in, ex);
      return null;
    }
  }

}
