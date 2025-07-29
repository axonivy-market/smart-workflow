package com.axonivy.utils.ai.tools;

import java.util.EnumSet;
import java.util.List;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.ProcessKind;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@SuppressWarnings("restriction")
class IvyToolsProcesses {

  static List<CallSubStart> toolStarts() {
    // TODO: craft with APIs that are public accessible
    IProcessManager man = ch.ivyteam.ivy.process.rdm.IProcessManager.instance();
    var processes = man.getProjectDataModelFor(IProcessModelVersion.current());
    return processes.searchAll(EnumSet.of(ProcessKind.CALLABLE_SUB))
        .tag("tool")
        .type(CallSubStart.class)
        .findDeep();
  }

}
