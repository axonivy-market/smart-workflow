package com.axonivy.utils.smart.workflow.scripting.internal;

import java.util.Optional;
import java.util.function.Predicate;

import ch.ivyteam.ivy.macro.IMacroExpanderManager;
import ch.ivyteam.ivy.macro.MacroException;
import ch.ivyteam.ivy.request.IProcessModelVersionRequest;
import ch.ivyteam.ivy.scripting.dataclass.mapper.IvyScriptEngineMapper;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.language.IIvyScriptEngine;
import ch.ivyteam.ivy.security.exec.Sudo;

public class MacroExpander {

  private final IIvyScriptContext context;

  public MacroExpander(IIvyScriptContext context) {
    this.context = context;
  }

  public Optional<String> expand(String template) {
    var expander = IMacroExpanderManager.instance().getMacroExpanderInstance();
    try {
      var expanded = expander.expandMacros(template, context, getIvyScriptEngine(context));
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (MacroException ex) {
      return Optional.empty();
    }
  }

  private static IIvyScriptEngine getIvyScriptEngine(IIvyScriptContext context) {
    var processRequest = (IProcessModelVersionRequest) context.getObjectOrNull("request");
    if (processRequest == null) {
      throw new IllegalArgumentException("Context does not contain variable request");
    }
    return Sudo.get(() -> IvyScriptEngineMapper.from(processRequest.project()));
  }

}
