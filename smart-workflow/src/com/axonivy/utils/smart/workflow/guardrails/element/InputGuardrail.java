package com.axonivy.utils.smart.workflow.guardrails.element;

import java.net.URI;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionDetector;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.macro.MacroException;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;

public class InputGuardrail extends AbstractUserProcessExtension implements IconDecorator {

  interface Variable {
    String RESULT = "result";
  }

  interface Conf {
    String INPUT = "input";
    String MAP_TO = "resultMapping";
  }

  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    var query = expand(context, Conf.INPUT);
    
    PromptInjectionDetector injectionDetector = new PromptInjectionDetector();
    var result = injectionDetector.validate(query.orElse(StringUtils.EMPTY));
    var mapTo = getConfig().get(Conf.MAP_TO);

    if (mapTo != null) {
      String mapIt = mapTo + "=result";
      try {
        declareAndInitializeVariable(context, Variable.RESULT, result.getClass().getName(), result);
        executeIvyScript(context, mapIt);
      } catch (Exception ex) {
        Ivy.log().error("Failed to map result to " + mapTo, ex);
      }
    }

    return in;
  }

  private Optional<String> expand(IIvyScriptContext context, String confKey) {
    try {
      var expanded = expandMacros(context, getConfig().get(confKey));
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (MacroException ex) {
      return Optional.empty();
    }
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.group("Message")
        .add(ui.label("Input").create())
        .add(ui.textField(Conf.INPUT).multiline().create())
        .create();

      ui.group("Guardrail")
          .add(ui.label("Prompt Injection Detector").create()) // TODO will be replace by a 'guardrail selector' text
                                                               // field later
        .create();
      
      ui.group("Output")
      .add(ui.label("Map result to:").create())
      .add(ui.scriptField(Conf.MAP_TO).requireType(Object.class).create())
      .add(ui.label("Result type must be com.axonivy.utils.smart.workflow.guardrails.GuardrailResult").create())
      .create();
    }

  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/blue_shield.png");
  }
}