package com.axonivy.utils.ai.tools;

import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.tools.internal.IvyToolsProcesses;

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

  public interface Conf {
    String QUERY = "query";
  }

  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    String query = getConfig().get(Conf.QUERY); // execute scripted?

    var model = new OpenAiServiceConnector()
        .buildOpenAiModel().build();

    var supporter = AiServices.builder(SupportAgent.class)
        .chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider())
        .build();
    var response2 = supporter.chat(query);

    Ivy.log().info("Agent response: " + response2);
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
      ui.label("You have the following tools ready to assist you:\n" + toolList())
          .multiline()
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
        return new IvyToolsProcesses(pmv).toolStarts().stream()
            .map(CallSubStart::getSignature)
            .map(tool -> "- " + tool.getName() + tool.getInputParameters().stream().map(VariableDesc::getName).toList())
            .collect(Collectors.joining("\n"));
      } catch (Exception ex) {
        return "";
      }
    }
  }

}
