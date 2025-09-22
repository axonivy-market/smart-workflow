package com.axonivy.utils.smart.workflow.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

public final class JsonUtils {

  static ObjectMapper objectMapper;

  public static ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    return objectMapper;
  }

  public static <T> List<T> jsonValueToEntities(String jsonValue, Class<T> classType) {
    if (StringUtils.isBlank(jsonValue)) {
      return new ArrayList<T>();
    }
    try {
      return getObjectMapper().readValue(jsonValue,
          getObjectMapper().getTypeFactory().constructCollectionType(List.class, classType));
    } catch (IOException e) {
      Ivy.log().error("Failed to convert JSON to entities: " + e);
    }
    return new ArrayList<T>();
  }
}
