package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;

public class IvyToolsProcesses {

  public static List<SubProcessCallStartEvent> toolStarts() {
    return SubProcessCallStartEvent.find(SubProcessSearchFilter.create()
        .setSearchScope(SearchScope.PROJECT_AND_ALL_REQUIRED)
        .taggedAs("tool")
        .toFilter());
  }

}
