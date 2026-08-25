package com.axonivy.utils.smart.workflow;

import java.net.URI;
import java.util.Map;

import com.axonivy.utils.smart.workflow.program.internal.AgentCallExecutor;
import com.axonivy.utils.smart.workflow.program.internal.AgentConfigMigrator;
import com.axonivy.utils.smart.workflow.program.internal.AgentEditor;

import ch.ivyteam.ivy.process.program.activity.AbortableExecution;
import ch.ivyteam.ivy.process.program.activity.ProgramExecutor;
import ch.ivyteam.ivy.process.program.element.ProgramIconDecorator;
import ch.ivyteam.ivy.process.program.migrate.ProgramConfigMigrator;
import ch.ivyteam.ivy.process.program.ui.ProgramEditorUi;
import ch.ivyteam.ivy.process.program.ui.ProgramUiBuilder;

public class AgenticProcessCall implements ProgramExecutor, ProgramEditorUi, ProgramIconDecorator, ProgramConfigMigrator {

  @Override
  public AbortableExecution newExecution() {
    return context -> new AgentCallExecutor(context).execute();
  }

  @Override
  public void editor(ProgramUiBuilder ui) {
    new AgentEditor().editor(ui);
  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/agent.png");
  }

  @Override
  public Map<String, String> migrateConfig(Map<String, String> config) {
    return new AgentConfigMigrator().migrateConfig(config);
  }
}
