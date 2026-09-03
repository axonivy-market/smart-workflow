package com.axonivy.utils.smart.workflow.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class JsonUtils {

  static final ObjectMapper objectMapper =
      JsonMapper.builderWithJackson2Defaults()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static <T> List<T> jsonValueToEntities(String jsonValue, Class<T> classType) {
    if (StringUtils.isBlank(jsonValue)) {
      return new ArrayList<T>();
    }
    try {
      return getObjectMapper().readValue(jsonValue,
          getObjectMapper().getTypeFactory().constructCollectionType(List.class, classType));
    } catch (Exception e) {
      Ivy.log().error("Failed to convert JSON to entities: " + e);
    }
    return new ArrayList<T>();
  }
}
