package com.axonivy.utils.smart.workflow;

import java.net.URI;

import ch.ivyteam.ivy.process.extension.exec.ProgramContext;
import ch.ivyteam.ivy.process.extension.exec.ProgramExecutor;
import ch.ivyteam.ivy.process.extension.ui.ConfigurableUi;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;

public class AgenticProcessCall
    implements ProgramExecutor, ConfigurableUi, IconDecorator {

  interface Conf {
    String SYSTEM = "system";
    String QUERY = "query";
    String TOOLS = "tools";
    String MODEL = "model";
    String PROVIDER = "provider";
    String OUTPUT = "resultType";
    String MAP_TO = "resultMapping";
  }

  @Override
  public void execute(ProgramContext context) {
    new AgentExecutor(context).chat();
  }

  @Override
  public void initUiFields(ExtensionUiBuilder ui) {
    new AgentConfigurationEditor().initUiFields(ui);
  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/agent.png");
  }

}
