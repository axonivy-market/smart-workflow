package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;

import ch.ivyteam.ivy.process.call.SubProcessCallStart;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;

@SuppressWarnings("restriction")
public class IvyToolsProcesses {

  public static List<SubProcessCallStart> toolStarts() {
    return SubProcessCallStart.find(SubProcessSearchFilter.create()
        .setSearchScope(SearchScope.PROJECT_AND_ALL_REQUIRED)
        .taggedAs("tool")
        .toFilter());
  }

}
