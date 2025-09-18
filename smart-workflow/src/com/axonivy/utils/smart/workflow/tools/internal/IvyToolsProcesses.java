package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.EnumSet;
import java.util.List;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.ProcessKind;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.rdm.IProjectProcessManager;

@SuppressWarnings("restriction")
public class IvyToolsProcesses {

  private final IProjectProcessManager processes;

  public IvyToolsProcesses(IProcessModelVersion pmv) {
    IProcessManager man = ch.ivyteam.ivy.process.rdm.IProcessManager.instance();
    this.processes = man.getProjectDataModelFor(pmv);
  }

  public List<CallSubStart> toolStarts() {
    // TODO: craft with APIs that are public accessible
    return processes.searchAll(EnumSet.of(ProcessKind.CALLABLE_SUB))
        .tag("tool")
        .type(CallSubStart.class)
        .findDeep();
  }

}
