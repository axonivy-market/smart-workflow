package com.axonivy.utils.smart.workflow.utils;

public final class ModelOptionUtils {
  private static final Double DEFAULT_TEMPERATURE = 0.0;

  public static Double toTemperature(String temperatureStr) {
    try {
      Double temperature = Double.valueOf(temperatureStr);
      return Double.isFinite(temperature) && temperature >= 0 ? temperature : DEFAULT_TEMPERATURE;
    } catch (NumberFormatException ex) {
      return DEFAULT_TEMPERATURE;
    }
  }
}
