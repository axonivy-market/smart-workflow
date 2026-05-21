package com.axonivy.utils.smart.workflow.spi.internal;

import org.apache.commons.lang3.reflect.MethodUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.project.model.Project;

public class ProjectClassLoader {

  private static final String JAVA_RUNTIME = "ch.ivyteam.ivy.java.JavaRuntime";

  public static ClassLoader current() {
    return of(IProcessModelVersion.current());
  }

  public static ClassLoader of(IProcessModelVersion pmv) {
    try {
      var javaConf = Class.forName(JAVA_RUNTIME);
      var of = MethodUtils.getMethodObject(javaConf, "of", Project.class);
      var local = of.invoke(null, pmv.project());
      var loader = MethodUtils.getMethodObject(javaConf, "getClassLoader");
      return (ClassLoader) loader.invoke(local);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
