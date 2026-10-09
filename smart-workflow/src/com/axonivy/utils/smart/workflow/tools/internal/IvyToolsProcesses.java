package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;

public class IvyToolsProcesses {

  private SearchScope scope = SearchScope.PROJECT_AND_ALL_REQUIRED;

  public IvyToolsProcesses scope(SearchScope scope) {
    this.scope = scope;
    return this;
  }

  public List<SubProcessCallStartEvent> toolStarts() {
    return SubProcessCallStartEvent.find(SubProcessSearchFilter.create()
        .setSearchScope(scope)
        .taggedAs("tool")
        .toFilter());
  }

}
