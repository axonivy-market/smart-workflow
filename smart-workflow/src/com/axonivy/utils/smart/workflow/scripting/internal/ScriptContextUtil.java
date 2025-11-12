package com.axonivy.utils.smart.workflow.scripting.internal;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.scripting.dataclass.IDataClassManager;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;

public class ScriptContextUtil {

  private final IIvyScriptContext context;

  public ScriptContextUtil(IIvyScriptContext context) {
    this.context = context;
  }

  @SuppressWarnings("restriction")
  public void declareVariable(String name, Object response) {
    // TODO clean public accessor for class-repo
    try {
      var project = IProcessModelVersion.current().project();
      var repo = IDataClassManager.instance().getProjectDataModelFor(project).getIvyScriptClassRepository();
      context.declareVariable(name, repo.getIvyClassForType(response.getClass()));
      context.setObject(name, response);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to declare variable " + name, ex);
    }
  }

}
