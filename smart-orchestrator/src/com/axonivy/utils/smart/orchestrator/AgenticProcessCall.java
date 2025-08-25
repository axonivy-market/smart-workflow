package com.axonivy.utils.smart.orchestrator;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.orchestrator.connector.OpenAiServiceConnector;
import com.axonivy.utils.smart.orchestrator.output.DynamicAgent;
import com.axonivy.utils.smart.orchestrator.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.orchestrator.scripting.internal.MacroExpander;
import com.axonivy.utils.smart.orchestrator.scripting.internal.ScriptContextUtil;
import com.axonivy.utils.smart.orchestrator.tools.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.orchestrator.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
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

  interface Conf {
    String SYSTEM = "system";
    String QUERY = "query";
    String TOOLS = "tools";
    String MODEL = "model";
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
    var query = expand(context, Conf.QUERY);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return in; // early abort; user is still testing with empty values
    }

    var modelBuilder = OpenAiServiceConnector.buildOpenAiModel();

    List<String> toolFilter = execute(context, Conf.TOOLS, List.class).orElse(null);
    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    var structured = execute(context, Conf.OUTPUT, Class.class);
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
      modelBuilder.responseFormat("json_schema");
    }
    var modelName = execute(context, Conf.MODEL, String.class);
    modelName.ifPresent(modelBuilder::modelName);

    var model = modelBuilder.build();
    var agentBuilder = AiServices.builder(agentType)
        .chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter));

    var systemMessage = expand(context, Conf.SYSTEM);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(memId -> systemMessage.get());
    }

    var agent = agentBuilder.build();
    var result = agent.chat(query.get());

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

  private <T> Optional<T> execute(IIvyScriptContext context, String configKey, Class<T> returnType) {
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

  private Optional<String> expand(IIvyScriptContext context, String confKey) {
    return new MacroExpander(context).expand(getConfig().get(confKey));
  }

  public static class Editor extends UiEditorExtension {

    @Override
    public void initUiFields(ExtensionUiBuilder ui) {
      ui.label("How can I assist you today?").create();
      ui.textField(Conf.QUERY)
          .multiline()
          .create();
      ui.label("System message:").create();
      ui.textField(Conf.SYSTEM)
          .create();
      ui.label("You have the following tools ready to assist you:\n" + toolList() + "\n\n"
          + "Select the available tools, or keep empty to use all:")
          .multiline()
          .create();
      ui.scriptField(Conf.TOOLS)
          .requireType(List.class)
          .create();

      ui.label("Model: (optional; defaults in variables.yaml)").create();
      ui.scriptField(Conf.MODEL).requireType(String.class).create();

      ui.label("Expect result of type:").create();
      ui.scriptField(Conf.OUTPUT).requireType(Class.class).create();

      ui.label("Map result to:").create();
      ui.scriptField(Conf.MAP_TO).create();
    }

    @SuppressWarnings("restriction")
    private String toolList() {
      var toolProcesses = Optional.ofNullable(IProcessModelVersion.current()).map(IvyToolsProcesses::new);
      if (toolProcesses.isEmpty()) {
        return "";
      }
      try {

        return toolProcesses.get()
            .toolStarts().stream()
            .map(CallSubStart::getSignature)
            .map(tool -> "- " + tool.getName() + tool.getInputParameters().stream().map(VariableDesc::getName).toList())
            .collect(Collectors.joining("\n"));
      } catch (Exception ex) {
        return "";
      }
    }
  }
}
