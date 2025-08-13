package com.axonivy.utils.ai.tools;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.agent.IvyToolRunner;
import com.axonivy.utils.ai.agent.Planner;
import com.axonivy.utils.ai.memory.AgentChatMemoryProvider;
import com.axonivy.utils.ai.tools.internal.IvySubProcessToolSpecs;
import com.axonivy.utils.ai.tools.internal.IvyToolsProcesses;
import com.axonivy.utils.ai.utils.IdGenerationUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.java.JavaConfigurationNavigationUtil;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.Json;

public class ReactAgenticProcessCall extends AbstractUserProcessExtension {

  public interface Conf {
    String QUERY = "query";
    String PLANNING_INSTRUCTIONS = "planningInstructions";
    String GOAL = "goal";
    String TOOLS = "tools";
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    String query = parseInput(in);
    Integer maxIterations = 15;

    var selectedTools = Optional.ofNullable(getConfig().get(Conf.TOOLS)).filter(Predicate.not(String::isBlank));
    List<String> toolFilter = null;
    if (selectedTools.isPresent()) {
      try {
        toolFilter = (List<String>) executeIvyScript(context, selectedTools.get());
      } catch (Exception ex) {
        Ivy.log().error("Failed to filter tools from " + selectedTools.get(), ex);
      }
    }

    var toolProvider = new IvySubProcessToolsProvider().filtering(toolFilter);
    String toolInfos = getToolInfos(toolFilter);

    String goal = getConfig().get(Conf.GOAL);
    String planningInstructions = getConfig().get(Conf.PLANNING_INSTRUCTIONS);

    AgentChatMemoryProvider memoryProvider = new AgentChatMemoryProvider();
    String runUuid = IdGenerationUtils.generateRandomId();
    memoryProvider.createNewMemory(runUuid);

    Planner planner = new Planner(memoryProvider, runUuid);

    planner.createPlan(goal, query, toolInfos, planningInstructions);

    IvyToolRunner runner = new IvyToolRunner(memoryProvider, runUuid, toolProvider);
    runner.run(query, maxIterations);
    return in;
  }

  private String getToolInfos(List<String> toolFilter) {
    List<ToolSpecification> toolSpecs = IvySubProcessToolSpecs.find();
    if (CollectionUtils.isNotEmpty(toolFilter)) {
      toolSpecs = toolSpecs.stream().filter(spec -> toolFilter.contains(spec.name())).toList();
    }

    StringBuilder builder = new StringBuilder();
    for (var toolSpec : toolSpecs) {
      builder.append(String.format("Name: %s", toolSpec.name())).append(System.lineSeparator())
          .append(String.format("Description: %s", toolSpec.description())).append(System.lineSeparator());

      if (toolSpec.parameters() != null) {
        builder.append("Parameters: ").append(System.lineSeparator());
        for (var entry : toolSpec.parameters().properties().entrySet()) {
          builder.append(String.format("  - %s : %s", entry.getKey(), entry.getValue().description()))
              .append(System.lineSeparator());
        }
      }

      builder.append(System.lineSeparator());
    }
    return builder.toString().strip();
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {

      ui.label("The goal this agent should achieve").create();
      ui.textField(Conf.GOAL).multiline().create();

      ui.label("Agent Input").create();
      ui.scriptField(Conf.QUERY).multiline().requireType(String.class).create();

      // Instructions section
      ui.label("Business context").create();
      ui.label("By providing business context, you help the agent \nplan and execute task better.").multiline()
          .create();
      ui.textField(Conf.PLANNING_INSTRUCTIONS).multiline().create();

      ui.label("You have the following tools ready to assist you:\n" + toolList() + "\n\n"
          + "Select the available tools, or keep empty to use all:")
          .multiline()
          .create();
      ui.scriptField(Conf.TOOLS)
          .requireType(List.class)
          .create();
    }

    @SuppressWarnings("restriction")
    private String toolList() {
      IProcessModelVersion pmv = JavaConfigurationNavigationUtil.getProcessModelVersion(Editor.class);
      List<IProcessModelVersion> user = pmv.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.DEPENDENT);
      if (!user.isEmpty()) {
        pmv = user.get(0); // TOOD: smarter; can I know my calling process?
      }

      try {
        return new IvyToolsProcesses(pmv).toolStarts().stream().map(CallSubStart::getSignature)
            .map(tool -> "- " + tool.getName() + tool.getInputParameters().stream().map(VariableDesc::getName).toList())
            .collect(Collectors.joining("\n"));
      } catch (Exception ex) {
        return "";
      }
    }
  }

  private String parseInput(CompositeObject in) {
    try {
      Object inputObj = in.get(getConfig().get(Conf.QUERY).substring(3));
      if (inputObj != null) {
        return Json.toJson(inputObj);
      }
      // If cannot find object, assume that the content of the field is a String
      return getConfig().get(Conf.QUERY);
    } catch (NoSuchFieldException e) {
      return StringUtils.EMPTY;
    }
  }
}
