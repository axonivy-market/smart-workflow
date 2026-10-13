package com.axonivy.utils.smart.workflow.model.utils;

import java.util.Optional;

public final class ModelOptionUtils {
  public static final Double DEFAULT_TEMPERATURE = 0.0;

  public static Double toTemperature(String temperatureStr) {
    if (temperatureStr == null || temperatureStr.isBlank()) {
      return DEFAULT_TEMPERATURE;
    }
    try {
      Double temperature = Double.valueOf(temperatureStr);
      return Double.isFinite(temperature) && temperature >= 0 ? temperature : DEFAULT_TEMPERATURE;
    } catch (NumberFormatException ex) {
      return DEFAULT_TEMPERATURE;
    }
  }

  public static Double resolveTemperature(Double temperature) {
    return Optional.ofNullable(temperature).orElse(DEFAULT_TEMPERATURE);
  }
}
