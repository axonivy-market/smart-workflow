package com.axonivy.utils.smart.workflow;

import java.net.URI;

import com.axonivy.utils.smart.workflow.program.internal.AgentCallExecutor;
import com.axonivy.utils.smart.workflow.program.internal.AgentEditor;

import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;
import ch.ivyteam.ivy.process.program.activity.AbortableExecution;
import ch.ivyteam.ivy.process.program.activity.ProgramExecutor;

public class AgenticProcessCall implements ProgramExecutor, IconDecorator {

  @Override
  public AbortableExecution newExecution() {
    return context -> new AgentCallExecutor(context).execute();
  }

  public static class Editor extends UiEditorExtension {
    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      new AgentEditor().initUiFields(ui);
    }
  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/agent.png");
  }
}
