package com.axonivy.utils.smart.workflow.test.utils;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public final class TestUtils {

  /** System property / env var holding the comma-separated list of providers to run E2E tests for. */
  public static final String PROVIDERS_PROPERTY = "providers";

  /**
   * Get system property by name
   *
   * @param propertyName
   * @return
   */
  public static String getSystemProperty(String propertyName) {
    return System.getProperty(propertyName, System.getenv(propertyName));
  }

  /**
   * Whether E2E tests for the given provider should run for this build.
   *
   * Reads the {@value #PROVIDERS_PROPERTY} property (comma-separated provider names). A blank value
   * runs every provider; otherwise only the listed providers run (case-insensitive).
   *
   * @param providerName provider name as exposed by its {@code ChatModelProvider.NAME}
   * @return {@code true} if the provider is selected (or no selection was made)
   */
  public static boolean isProviderEnabled(String providerName) {
    String selected = getSystemProperty(PROVIDERS_PROPERTY);
    if (StringUtils.isBlank(selected)) {
      return true;
    }
    return Arrays.stream(selected.split(","))
        .map(String::trim)
        .anyMatch(name -> name.equalsIgnoreCase(providerName));
  }
}
