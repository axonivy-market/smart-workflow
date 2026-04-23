package com.axonivy.utils.smart.workflow.spi.internal;

import java.util.function.Predicate;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public final class SpiProject {

  private interface SmartWorkflow {
    String GROUP_ID = "com.axonivy.utils.ai";
    String ARTIFACT_ID = "smart-workflow";
    String LIBRARY_ID = GROUP_ID + ":" + ARTIFACT_ID;
  }

  public static IProcessModelVersion getSmartWorkflowPmv() {
    Predicate<IProcessModelVersion> smartWorkflow = pmv -> 
      SmartWorkflow.LIBRARY_ID.equals(pmv.getLibraryId());
    var current = IProcessModelVersion.current();
    if (smartWorkflow.test(current)) {
      return current;
    }
    return current.getAllRequiredProcessModelVersions()
        .filter(smartWorkflow)
        .findAny()
        .orElseThrow();
  }
}
