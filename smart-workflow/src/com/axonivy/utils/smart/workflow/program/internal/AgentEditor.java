package com.axonivy.utils.smart.workflow.program.internal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;
import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.process.call.StartDescriptor;
import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.program.ui.ProgramUiBuilder;
import ch.ivyteam.ivy.process.program.ui.select.SelectItem;

public class AgentEditor {

  public void editor(ProgramUiBuilder ui) {
    ui.group("✉️ Message")
        .add(ui.textField(Conf.QUERY)
          .label("User message:")
          .multiline()
          .create())
        .add(ui.textField(Conf.SYSTEM)
          .label("System message:")
          .multiline()
          .create())
        .create();

    ui.group("🛠️ Tools")
        .add(ui.multiSelect(Conf.TOOLS)
          .label("Available tools:")
          .items(toolList())
          .create())
        .create();

    ui.group("🛡️ Guardrails")
        .add(ui.label("Select guardrails to apply, or keep empty to use the default guardrails")
          .create())
        .add(ui.multiSelect(Conf.INPUT_GUARD_RAILS)
          .label("Input guardrails:")
          .items(inputGuardrailsList())
          .create())
        .add(ui.multiSelect(Conf.OUTPUT_GUARD_RAILS)
          .label("Output guardrails:")
          .items(outputGuardrailsList())
          .create())
        .create();

    ui.group("🧠️ Model")
        .add(ui.multiSelect(Conf.PROVIDER)
          .label("Provider:")
          .items(providersList())
          .help("Choose one of the supported providers. Keep empty to use default from variables.yaml")
          .create())
        .add(ui.scriptField(Conf.MODEL)
          .label("Model:")
          .help("Keep empty to use default from variables.yaml")
          .requireType(String.class)
          .create())
        .create();

    ui.group("➡️ Output")
        .add(ui.scriptField(Conf.OUTPUT)
          .label("Expect result of type:")
          .requireType(Class.class)
          .create())
        .add(ui.scriptField(Conf.MAP_TO)
          .label("Map result to:")
          .requireType(Object.class)
          .create())
        .create();
  }

  private List<SelectItem> toolList() {
    try {
      var ivyTools = IvyToolsProcesses
          .toolStarts().stream()
          .map(SubProcessCallStartEvent::description)
          .map(AgentEditor::toItem);
      return Stream.concat(ivyTools, javaToolNames()).toList();
    } catch (Exception ex) {
      return List.of();
    }
  }

  private static SelectItem toItem(StartDescriptor tool) {
    String params = tool.in().isEmpty() ? ""
        : tool.in().stream().map(StartParameter::name)
            .collect(Collectors.joining(", ", " (", ")"));
    return SelectItem.of(tool.name(), tool.name() + params, "🧰️", tool.description());
  }

  private Stream<SelectItem> javaToolNames() {
    try {
      var pmv = SpiProject.getSmartWorkflowPmv();
      return new SpiLoader(pmv).load(SmartWorkflowToolsProvider.class).stream()
          .flatMap(provider -> {
            var tools = provider.getTools();
            return tools == null ? Stream.empty() : tools.stream();
          })
          .map(t -> SelectItem.of(t.name(), t.name(), "☕️", t.description()));
    } catch (Exception ex) {
      return Stream.empty();
    }
  }

  private List<SelectItem> providersList() {
    return ChatModelFactory.providers().stream()
      .distinct()
      .map(provider -> SelectItem.of(provider.name()))
      .toList();
  }

  private List<SelectItem> inputGuardrailsList() {
    return GuardrailCollector.allInputGuardrailNames().stream()
        .map(name -> SelectItem.of(name, name, "➡️", null))
        .toList();
  }

  private List<SelectItem> outputGuardrailsList() {
    return GuardrailCollector.allOutputGuardrailNames().stream()
        .map(name -> SelectItem.of(name, name, "⬅️", null))
        .toList();
  }
}
