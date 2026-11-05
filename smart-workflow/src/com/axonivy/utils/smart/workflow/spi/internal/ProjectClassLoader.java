package com.axonivy.utils.smart.workflow.spi.internal;

import java.util.List;

import ch.ivyteam.ivy.application.project.Project;

public class ProjectClassLoader {

  public static ClassLoader current() {
    return of(Project.current());
  }

  public static ClassLoader of(Project pmv) {
    return ch.ivyteam.ivy.java.project.ProjectClassLoader.of(pmv);
  }

  /**
   * Loads from the given class loader first, then from the projects that use Smart Workflow. A tool can be passed at
   * runtime by a project the current one does not require, and its parameter types live in that project.
   */
  public static ClassLoader withApplicationFallback(ClassLoader primary) {
    List<ClassLoader> fallbacks = SpiProject.getSmartWorkflowPmv().allDependentProjects()
        .map(ProjectClassLoader::of)
        .toList();
    return new ClassLoader(primary) {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader fallback : fallbacks) {
          try {
            return fallback.loadClass(name);
          } catch (ClassNotFoundException ex) {
            // try the next project
          }
        }
        throw new ClassNotFoundException(name);
      }
    };
  }

}
