package com.axonivy.utils.smart.workflow.utils;

import java.util.function.Predicate;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;

public final class IvyUtils {
  private static final String SMART_WORKFLOW_PROJECT = "smart-workflow";

  public static IProcessModelVersion getSmartWorlflowPmv() {
    Predicate<IProcessModelVersion> smartWorkflow = pmv -> SMART_WORKFLOW_PROJECT.equals(pmv.getName());
    var current = IProcessModelVersion.current();
    if (smartWorkflow.test(current)) {
      return current;
    }
    return current.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.REQUIRED).stream()
        .filter(smartWorkflow).findAny().orElseThrow();
  }
}
