package com.axonivy.utils.smart.workflow.spi.internal;

import ch.ivyteam.ivy.application.project.Project;

public class ProjectClassLoader {

  public static ClassLoader current() {
    return of(Project.current());
  }

  public static ClassLoader of(Project pmv) {
    return ch.ivyteam.ivy.java.project.ProjectClassLoader.of(pmv);
  }

}
