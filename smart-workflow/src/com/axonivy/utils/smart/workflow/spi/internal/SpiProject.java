package com.axonivy.utils.smart.workflow.spi.internal;

import java.util.function.Predicate;

import ch.ivyteam.ivy.application.project.Project;

public final class SpiProject {

  private interface SmartWorkflow {
    String GROUP_ID = "com.axonivy.utils.ai";
    String ARTIFACT_ID = "smart-workflow";
    String LIBRARY_ID = GROUP_ID + ":" + ARTIFACT_ID;
  }

  public static Project getSmartWorkflowPmv() {
    Predicate<Project> smartWorkflow = pmv -> 
      SmartWorkflow.LIBRARY_ID.equals(pmv.mavenCoordinates().id());
    var current = Project.current();
    if (smartWorkflow.test(current)) {
      return current;
    }
    return current.allRequiredProjects()
        .filter(smartWorkflow)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(String.format(
            "Cannot resolve Smart Workflow PMV. Expected libraryId='%s', currentLibraryId='%s'",
            SmartWorkflow.LIBRARY_ID,
            current.mavenCoordinates().id())));
  }
}
