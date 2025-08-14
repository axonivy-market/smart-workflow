package com.axonivy.utils.ai.tools;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.tools.internal.IvyToolsProcesses;
import com.axonivy.utils.ai.tools.internal.ScriptContextUtil;

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
import dev.langchain4j.service.AiServices;

public class AgenticProcessCall extends AbstractUserProcessExtension {

  interface Variable {
    String RESULT = "result";
  }

  public interface Conf {
    String QUERY = "query";
    String TOOLS = "tools";
    String MAP_TO = "resultMapping";
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    String query = getConfig().get(Conf.QUERY); // execute scripted?

    var model = new OpenAiServiceConnector()
        .buildOpenAiModel().build();

    var selectedTools = Optional.ofNullable(getConfig().get(Conf.TOOLS))
        .filter(Predicate.not(String::isBlank));
    List<String> toolFilter = null;
    if (selectedTools.isPresent()) {
      try {
        toolFilter = (List<String>) executeIvyScript(context, selectedTools.get());
      } catch (Exception ex) {
        Ivy.log().error("Failed to filter tools from " + selectedTools.get(), ex);
      }
    }

    var supporter = AiServices.builder(SupportAgent.class)
        .chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter))
        .build();
    var result = supporter.chat(query);

    var mapTo = getConfig().get(Conf.MAP_TO);
    if (mapTo != null) {
      String mapIt = mapTo + "=result";
      try {
        new ScriptContextUtil(context).declareVariable(Variable.RESULT, result);
        executeIvyScript(context, mapIt);
      } catch (Exception ex) {
        Ivy.log().error("Failed to map result to " + mapTo, ex);
      }
    }

    Ivy.log().info("Agent response: " + result);
    return in;
  }

  interface SupportAgent {
    String chat(String query);
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.label("How can I assist you today?").create();
      ui.scriptField(Conf.QUERY)
          .multiline()
          .requireType(String.class)
          .create();
      ui.label("You have the following tools ready to assist you:\n" + toolList() + "\n\n"
          + "Select the available tools, or keep empty to use all:")
          .multiline()
          .create();
      ui.scriptField(Conf.TOOLS)
          .requireType(List.class)
          .create();

      ui.label("Map result to:").create();
      ui.scriptField(Conf.MAP_TO).create();
    }

    @SuppressWarnings("restriction")
    private String toolList() {
      IProcessModelVersion pmv = getPmv();
      try {
        return new IvyToolsProcesses(pmv).toolStarts().stream()
            .map(CallSubStart::getSignature)
            .map(tool -> "- " + tool.getName() + tool.getInputParameters().stream().map(VariableDesc::getName).toList())
            .collect(Collectors.joining("\n"));
      } catch (Exception ex) {
        return "";
      }
    }

    private IProcessModelVersion getPmv() {
      return Optional.ofNullable(IProcessModelVersion.current())
          .orElseGet(this::preDiavolezzaSprint13);
    }

    private IProcessModelVersion preDiavolezzaSprint13() {
      var pmv = JavaConfigurationNavigationUtil.getProcessModelVersion(Editor.class);
      List<IProcessModelVersion> user = pmv.getAllRelatedProcessModelVersions(ProcessModelVersionRelation.DEPENDENT);
      if (!user.isEmpty()) {
        pmv = user.get(0); // TODO: delete me once Sprint13 is public available
      }
      return pmv;
    }
  }
}
