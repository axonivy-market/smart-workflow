package com.axonivy.utils.smart.orchestrator.scripting.internal;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.scripting.dataclass.IDataClassManager;
import ch.ivyteam.ivy.scripting.exceptions.IvyScriptException;
import ch.ivyteam.ivy.scripting.exceptions.invocation.IvyScriptVariableAlreadyDeclaredException;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;

public class ScriptContextUtil {

  private final IIvyScriptContext context;

  public ScriptContextUtil(IIvyScriptContext context) {
    this.context = context;
  }

  @SuppressWarnings("restriction")
  public void declareVariable(String name, Object response) throws IvyScriptVariableAlreadyDeclaredException, IvyScriptException {
    // TODO clean public accessor for class-repo
    var repo = IDataClassManager.instance().getProjectDataModelFor(IProcessModelVersion.current()).getIvyScriptClassRepository();
    context.declareVariable(name, repo.getIvyClassForType(response.getClass()));
    context.setObject(name, response);
  }

}
