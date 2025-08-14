package com.axonivy.utils.ai.core;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.agent.IvySupervisor;
import com.axonivy.utils.ai.agent.Planner;
import com.axonivy.utils.ai.core.internal.IvyToolsProcesses;
import com.axonivy.utils.ai.memory.AgentChatMemoryProvider;
import com.axonivy.utils.ai.utils.IdGenerationUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;
import ch.ivyteam.ivy.java.JavaConfigurationNavigationUtil;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import dev.langchain4j.internal.Json;

public class ReactAgenticProcessCall extends AbstractUserProcessExtension {

  public interface Conf {
    String QUERY = "query";
    String PLANNING_INSTRUCTIONS = "planningInstructions";
    String GOAL = "goal";
    String TOOLS = "tools";
  }

  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    String query = parseInput(in);
    Integer maxIterations = 15;

    String goal = getConfig().get(Conf.GOAL);

    // Initialize chat memory provider and store
    AgentChatMemoryProvider memoryProvider = new AgentChatMemoryProvider();
    String runUuid = IdGenerationUtils.generateRandomId();
    memoryProvider.createNewMemory(runUuid);

    // Format planning instructions
    String planningInstructions = getConfig().get(Conf.PLANNING_INSTRUCTIONS);

    Planner planner = new Planner(memoryProvider, runUuid);

    // Create plan & inject it to the chat history, so other agents can see the plan
    planner.createPlan(goal, query, goal, planningInstructions);

    IvySupervisor supervisor = new IvySupervisor(memoryProvider, runUuid, new IvySubProcessToolsProvider());
    supervisor.run(query, maxIterations);
    memoryProvider.persist(runUuid);
    return in;
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
