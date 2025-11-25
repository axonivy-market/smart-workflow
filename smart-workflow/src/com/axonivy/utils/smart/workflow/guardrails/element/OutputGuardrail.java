package com.axonivy.utils.smart.workflow.guardrails.element;

import java.net.URI;

import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;

public class OutputGuardrail extends AbstractUserProcessExtension implements IconDecorator {
  interface Variable {
    String RESULT = "result";
  }

  interface Conf {
    String MESSAGE = "message";
  }

  @SuppressWarnings({ "unchecked" })
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    return in;
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.group("Message")
        .add(ui.label("User message").create())
        .add(ui.textField(Conf.MESSAGE).multiline().create())
        .create();

      ui.group("Guardrail")
        .add(ui.label("Prompt Injection Detector").create())
        .create();
     
    }

  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/red_shield.png");
  }
}