package com.axonivy.utils.smart.workflow.utils;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyVar {
  
  public static boolean bool(String name) {
    return "true".equals(Ivy.var().get(name));
  }

}

