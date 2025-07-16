package com.axonivy.utils.ai.utils;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public final class IvyVariableUtils {

  /**
   * Resolves variable references like ${AI.OpenAI.APIKey} to actual values from
   * Ivy variables.
   * 
   * Supported patterns:
   * - ${variable.path} -> Ivy.var().get("variable.path")
   * - Regular strings are returned as-is
   */
  public static String resolveVariableReference(String value) {
    if (value == null || value.trim().isEmpty()) {
      return value;
    }

    // Check if the value is a variable reference pattern: ${variable.path}
    if (value.startsWith("${") && value.endsWith("}")) {
      // Extract the variable path (remove ${ and })
      String variablePath = value.substring(2, value.length() - 1).trim();
      if (variablePath.isEmpty()) {
        return StringUtils.EMPTY;
      }
      return Ivy.var().get(variablePath);
    }

    // If not a variable reference, return as-is
    return value;
  }
}
