package com.axonivy.utils.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import com.axonivy.utils.ai.tools.internal.IvyToolsProcesses;

import com.axonivy.utils.ai.core.AgentExecutor;
import com.axonivy.utils.ai.dto.ai.Instruction;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ProcessModelVersionRelation;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.java.JavaConfigurationNavigationUtil;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;

public class AgenticProcessCall extends AbstractUserProcessExtension {

  public interface Conf {
    String QUERY = "query";
    String PLANNING_INSTRUCTIONS = "planningInstructions";
    String INSTRUCTIONS = "instructions";
    String MAX_ITERATIONS = "maxIterations";
    String AGENT_ID = "agentId";
    String GOAL = "goal";
  }

  @SuppressWarnings("restriction")
  private static List<CallSubStart> toolList() {
    IProcessModelVersion pmv = JavaConfigurationNavigationUtil.getProcessModelVersion(Editor.class);
    List<IProcessModelVersion> user = pmv.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.DEPENDENT);
    if (!user.isEmpty()) {
      pmv = user.get(0); // TOOD: smarter; can I know my calling process?
    }

    try {
      return new IvyToolsProcesses(pmv).toolStarts().stream().toList();
    } catch (Exception ex) {
      return null;
    }
  }

  @SuppressWarnings("restriction")
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    Object inputObj = in.get(getConfig().get(Conf.QUERY).substring(3));
    if (inputObj == null) {
      inputObj = getConfig().get(Conf.QUERY);
    }
    Integer maxIterations = NumberUtils.toInt(getConfig().get(Conf.MAX_ITERATIONS), 15);

    String goal = getConfig().get(Conf.GOAL);

    List<Instruction> instructions = new ArrayList<>();
    instructions.add(Instruction.createPlanningInstruction(getConfig().get(Conf.PLANNING_INSTRUCTIONS)));
    List<CallSubStart> tools = toolList();

    for (var tool : tools) {
      String signatureStr = tool.getSignature().toSignatureString();
      String executionInstruction = getConfig().get(signatureStr);
      if (StringUtils.isNotBlank(executionInstruction)) {
        instructions.add(Instruction.createExecutionInstruction(signatureStr, executionInstruction));
      }
    }

    AgentExecutor executor = new AgentExecutor();
    executor.startExecution(inputObj, Ivy.session().getSessionUserName(), getConfig().get(Conf.AGENT_ID), tools,
        instructions, goal, maxIterations);

    /*
     * var model = new OpenAiServiceConnector() .buildOpenAiModel().build();
     * 
     * var supporterBuilder = AiServices.builder(SupportAgent.class)
     * .chatModel(model) .toolProvider(new IvyToolsProvider());
     * 
     * var supporter = supporterBuilder.build();
     */
    // var response2 = supporter.chat(query);

    // Ivy.log().info("Agent response: " + response2);
    return in;
  }

  interface SupportAgent {
    String chat(String query);
  }

  public static class Editor extends UiEditorExtension {

    @SuppressWarnings("restriction")
    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.label("Agent Id").create();
      ui.scriptField(Conf.AGENT_ID)
          .multiline()
          .create();
      
      ui.label("Goal").create();
      ui.textField(Conf.GOAL).multiline().create();

      ui.label("Agent Input").create();
      ui.scriptField(Conf.QUERY)
          .multiline()
          .requireType(String.class)
          .create();

      // Iteration count
      ui.label("Max Iterations").create();
      ui.scriptField(Conf.MAX_ITERATIONS)
          .requireType(Integer.class)
          .create();

      // Instructions section
      ui.label("Planning Instructions").create();
      ui.label("Configure instructions for planning phases").create();
      ui.textField(Conf.PLANNING_INSTRUCTIONS).multiline().create();

      ui.label("Tools").create();
      for (var toolStartable : AgenticProcessCall.toolList()) {
        ui.label(" " + toolStartable.getName()).create();
        String description = toolStartable.getDescription();
        if (StringUtils.isNotBlank(description)) {
          ui.label("  - " + description).create();
        }
        ui.label("  - Execution instructions").create();
        ui.textField(toolStartable.getSignature().toSignatureString()).multiline().create();
      }
    }
  }

}
