package com.axonivy.utils.smart.workflow.test.utils;

public final class TestUtils {

  /**
   * Get system property by name
   * 
   * @param propertyName
   * @return
   */
  public static String getSystemProperty(String propertyName) {
    return System.getProperty(propertyName, System.getenv(propertyName));
  }
}
