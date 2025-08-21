package com.axonivy.utils.ai.tools.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.scripting.dataclass.IProjectDataClassManager;
import ch.ivyteam.ivy.scripting.types.QualifiedTypeLoader;
import ch.ivyteam.ivy.scripting.types.QualifiedTypeLoader.QType;

@SuppressWarnings("restriction")
public class JsonProcessParameters {

  private static final Logger LOGGER = Logger.getLogger(JsonProcessParameters.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final QualifiedTypeLoader loader;

  public JsonProcessParameters(IProcessModelVersion pmv) {
    var repo = IProjectDataClassManager.of(pmv).getIvyScriptClassRepository();
    this.loader = new QualifiedTypeLoader(repo);
  }

  public Map<String, Object> readParams(List<VariableDesc> ins, String rawJsonArgs) {
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

  public Map<String, Object> toParams(List<VariableDesc> ins, JsonNode rawArgs) {
    var map = new LinkedHashMap<String, Object>();
    ins.stream().forEachOrdered(in -> {
      map.put(in.getName(), toValue(in, rawArgs));
    });
    return map;
  }

  private Object toValue(VariableDesc in, JsonNode rawArgs) {
    try {
      var typed = loader.load(new QType(in.getType().getName()));
      var jArg = rawArgs.get(in.getName());
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
