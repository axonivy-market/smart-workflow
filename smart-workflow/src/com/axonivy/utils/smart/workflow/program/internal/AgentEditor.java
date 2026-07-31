package com.axonivy.utils.smart.workflow.program.internal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
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
        .add(ui.label("User message:").create())
        .add(ui.textField(Conf.QUERY).multiline().create())
        .add(ui.label("System message:").create())
        .add(ui.textField(Conf.SYSTEM).multiline().create())
        .create();

    ui.group("🛠️ Tools")
        .add(ui.label("Select the available tools:").create())
        .add(ui.multiSelect(Conf.TOOLS).items(toolList()).create())
        .create();

    String inputGuardrailList = inputGuardrailsList();
    String outputGuardrailList = outputGuardrailsList();
    var guardrailsGroup = ui.group("🛡️ Guardrails");
    guardrailsGroup.add(ui.label("Select guardrails to apply, or keep empty to use the default guardrails").create());
    guardrailsGroup
        .add(ui.label("Available input guardrails:\n").create())
        .add(ui.label(inputGuardrailList).multiline().create())
        .add(ui.scriptField(Conf.INPUT_GUARD_RAILS).requireType(List.class).create());
    guardrailsGroup
        .add(ui.label("Available output guardrails:\n").create())
        .add(ui.label(outputGuardrailList).multiline().create())
        .add(ui.scriptField(Conf.OUTPUT_GUARD_RAILS).requireType(List.class).create());
    guardrailsGroup.create();

    ui.group("🧠️ Model")
        .add(ui.label("Provider").create())
        .add(ui.label(providersHelp()).multiline().create())
        .add(ui.scriptField(Conf.PROVIDER).requireType(String.class).create())
        .add(ui.label("Keep empty to use default from variables.yaml").create())
        .add(ui.label("Model").create())
        .add(ui.scriptField(Conf.MODEL).requireType(String.class).create())
        .add(ui.label("Keep empty to use default from variables.yaml").create())
        .create();

    ui.group("➡️ Output")
        .add(ui.label("Expect result of type:").create())
        .add(ui.scriptField(Conf.OUTPUT).requireType(Class.class).create())
        .add(ui.label("Map result to:").create())
        .add(ui.scriptField(Conf.MAP_TO).requireType(Object.class).create())
        .create();
  }

  private String providersHelp() {
    return "Choose one of the supported AI providers:\n" + providersList();
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

  private String providersList() {
    var providers = Optional.ofNullable(ChatModelFactory.providers());
    if (providers.isEmpty()) {
      return StringUtils.EMPTY;
    }
    return providers.get().stream().map(ChatModelProvider::name).distinct().collect(Collectors.joining(", "));
  }

  private String inputGuardrailsList() {
    return GuardrailCollector.allInputGuardrailNames().stream()
        .map(name -> "- " + name)
        .collect(Collectors.joining("\n"));
  }

  private String outputGuardrailsList() {
    return GuardrailCollector.allOutputGuardrailNames().stream()
        .map(name -> "- " + name)
        .collect(Collectors.joining("\n"));
  }
}
