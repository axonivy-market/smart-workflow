package com.axonivy.utils.smart.orchestrator.scripting.internal;

import static org.apache.commons.text.StringSubstitutor.DEFAULT_ESCAPE;

import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.text.StringSubstitutor;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.macro.IMacroExpanderManager;
import ch.ivyteam.ivy.macro.MacroException;
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
      var preExpand = new StringSubstitutor(a -> "<%=" + a + "%>", "<%=", "%>", DEFAULT_ESCAPE).replace(template);
      var expanded = expander.expandMacros(preExpand, context, getIvyScriptEngine());
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (MacroException ex) {
      return Optional.empty();
    }
  }

  private static IIvyScriptEngine getIvyScriptEngine() {
    return Sudo.get(() -> IvyScriptEngineMapper.from(IProcessModelVersion.current().project()));
  }

}
