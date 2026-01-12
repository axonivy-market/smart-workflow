package com.axonivy.utils.smart.workflow.program.internal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailProvider;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;

public class AgentEditor {

  public void initUiFields(ExtensionUiBuilder ui) {
    ui.group("Message")
        .add(ui.label("How can I assist you today?").create())
        .add(ui.textField(Conf.QUERY).multiline().create())
        .add(ui.label("System message:").create())
        .add(ui.textField(Conf.SYSTEM).multiline().create())
        .create();

    ui.group("Tools")
        .add(ui.label(toolsHelp()).multiline().create())
        .add(ui.scriptField(Conf.TOOLS).requireType(List.class).create())
        .create();

    String guardrailList = guardrailsList();
    if (StringUtils.isNotBlank(guardrailList)) {
      ui.group("Guardrails").add(ui.label("Available guardrails:\n").create())
          .add(ui.label(guardrailList).multiline().create())
          .add(ui.scriptField(Conf.INPUT_GUARD_RAILS).requireType(List.class).create())
          .add(ui.label("Select the guardrails to apply, or keep empty to use default guardrails").create()).create();
    }

    ui.group("Model")
        .add(ui.label("Provider").create())
        .add(ui.label(providersHelp()).multiline().create())
        .add(ui.scriptField(Conf.PROVIDER).requireType(String.class).create())
        .add(ui.label("Keep empty to use default from variables.yaml\r\n" + "").create())
        .add(ui.label("Model").create())
        .add(ui.scriptField(Conf.MODEL).requireType(String.class).create())
        .add(ui.label("Keep empty to use default from variables.yaml").create())
        .create();

    ui.group("Output")
        .add(ui.label("Expect result of type:").create())
        .add(ui.scriptField(Conf.OUTPUT).requireType(Class.class).create())
        .add(ui.label("Map result to:").create())
        .add(ui.scriptField(Conf.MAP_TO).requireType(Object.class).create())
        .create();
  }

  private String toolsHelp() {
    return "You have the following tools ready to assist you:\n" + toolList() + "\n\n"
        + "Select the available tools, or keep empty to use all:";
  }

  private String providersHelp() {
    return "Choose one of the supported AI providers:\n" + providersList();
  }

  @SuppressWarnings("restriction")
  private String toolList() {
    try {
      return IvyToolsProcesses
          .toolStarts().stream()
          .map(SubProcessCallStartEvent::description)
          .map(tool -> "- " + tool.name() + tool.in().stream().map(StartParameter::name).toList())
          .collect(Collectors.joining("\n"));
    } catch (Exception ex) {
      return "";
    }
  }

  private String providersList() {
    var providers = Optional.ofNullable(ChatModelFactory.providers());
    if (providers.isEmpty()) {
      return StringUtils.EMPTY;
    }
    return providers.get().stream().map(ChatModelProvider::name).distinct().collect(Collectors.joining(", "));
  }

  private String guardrailsList() {
    var providers = Optional.ofNullable(GuardrailProvider.providers());
    if (providers.isEmpty()) {
      return StringUtils.EMPTY;
    }
    return providers.get().stream().map(SmartWorkflowInputGuardrail::name).distinct()
        .map(name -> String.format("- %s", name))
        .collect(Collectors.joining("\n"));
  }
}
