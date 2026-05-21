package com.axonivy.utils.smart.workflow.spi.internal;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public class ProjectClassLoader {

  public static ClassLoader current() {
    return of(IProcessModelVersion.current());
  }

  public static ClassLoader of(IProcessModelVersion pmv) {
    return ch.ivyteam.ivy.java.project.ProjectClassLoader.of(pmv);
  }

}
