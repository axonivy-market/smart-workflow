package com.axonivy.utils.ai.tools;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.output.DynamicAgent;
import com.axonivy.utils.ai.output.internal.StructuredOutputAgent;
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
    String OUTPUT = "resultType";
    String MAP_TO = "resultMapping";
  }

  interface ChatAgent extends DynamicAgent<String> {
    @Override
    default String chat(String query) {
      return null;
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    var query = execute(context, Conf.QUERY, String.class);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return in; // early abort; user is still testing with empty values
    }

    var modelBuilder = new OpenAiServiceConnector().buildOpenAiModel();

    List<String> toolFilter = execute(context, Conf.TOOLS, List.class).orElse(null);
    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    var structured = execute(context, Conf.OUTPUT, Class.class);
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
      modelBuilder.responseFormat("json_schema");
    }

    var model = modelBuilder.build();
    var supporter = AiServices.builder(agentType)
        .chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter))
        .build();
    var result = supporter.chat(query.get());

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

  private <T> Optional<T> execute(IIvyScriptContext context, String configKey, @SuppressWarnings("unused") Class<T> returnType) {
    var value = Optional.ofNullable(getConfig().get(configKey))
        .filter(Predicate.not(String::isBlank));
    if (value.isEmpty()) {
      return Optional.empty();
    }
    try {
      var resolved = executeIvyScript(context, value.get());
      return Optional.ofNullable(resolved)
          .filter(returnType::isInstance)
          .map(returnType::cast);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to extract config '" + configKey + "' for value '" + value.get() + "'", ex);
    }
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

      ui.label("Expect result of type:").create();
      ui.scriptField(Conf.OUTPUT).requireType(Class.class).create();

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
