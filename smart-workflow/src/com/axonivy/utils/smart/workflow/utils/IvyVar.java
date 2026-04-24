package com.axonivy.utils.smart.workflow.utils;

import ch.ivyteam.ivy.environment.Ivy;
import org.apache.commons.lang3.math.NumberUtils;

public class IvyVar {
  
  public static boolean bool(String name) {
    return "true".equals(Ivy.var().get(name));
  }

  public static int integer(String name, int defaultValue) {
    return NumberUtils.toInt(Ivy.var().get(name), defaultValue);
  }

  public static double decimal(String name, double defaultValue) {
    return NumberUtils.toDouble(Ivy.var().get(name), defaultValue);
  }
}

