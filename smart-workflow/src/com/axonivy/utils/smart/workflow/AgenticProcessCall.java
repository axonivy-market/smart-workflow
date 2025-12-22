package com.axonivy.utils.smart.workflow;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailFactory;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder.GroupBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.service.AiServices;

public class AgenticProcessCall extends AbstractUserProcessExtension implements IconDecorator {

  interface Variable {
    String RESULT = "result";
  }

  interface Conf {
    String SYSTEM = "system";
    String QUERY = "query";
    String TOOLS = "tools";
    String MODEL = "model";
    String PROVIDER = "provider";
    String OUTPUT = "resultType";
    String MAP_TO = "resultMapping";
    String INPUT_GUARD_RAILS = "inputGuardrails";
    String OUTPUT_GUARD_RAILS = "outputGuardrails";
  }

  interface ChatAgent extends DynamicAgent<String> {
    @Override
    String chat(String query);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public CompositeObject perform(IRequestId requestId, CompositeObject in, IIvyScriptContext context) throws Exception {
    var query = expand(context, Conf.QUERY);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return in; // early abort; user is still testing with empty values
    }

    String providerName = execute(context, Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    String modelName = execute(context, Conf.MODEL, String.class).orElse(StringUtils.EMPTY);

    List<String> toolFilter = execute(context, Conf.TOOLS, List.class).orElse(null);
    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    var structured = execute(context, Conf.OUTPUT, Class.class);
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
    }
    var modelOptions = options()
        .modelName(modelName)
        .structuredOutput(structured.isPresent());
    var model = ChatModelFactory.createModel(modelOptions, providerName);

    var agentBuilder = AiServices.builder(agentType)
        .chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter));

    var systemMessage = expand(context, Conf.SYSTEM);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(memId -> systemMessage.get());
    }

    List<String> inputGuardrailNames = execute(context, Conf.INPUT_GUARD_RAILS, List.class).orElse(new ArrayList<>());
    List<InputGuardrail> inputGuardrails = inputGuardrailNames.stream()
        .map(guardrailName -> GuardrailFactory.create(guardrailName).orElse(null)).filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (CollectionUtils.isNotEmpty(inputGuardrails)) {
      agentBuilder.inputGuardrails(inputGuardrails);
    }

    var agent = agentBuilder.build();
    var result = agent.chat(query.get());

    var mapTo = getConfig().get(Conf.MAP_TO);
    if (mapTo != null) {
      String mapIt = mapTo + "=result";
      try {
        declareAndInitializeVariable(context, Variable.RESULT, result.getClass().getName(), result);
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
    try {
      var expanded = expandMacros(context, getConfig().get(confKey));
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static class Editor extends UiEditorExtension {

    @Override
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

      List<String> inputGuardrails = guardrailList(true);
      List<String> outputGuardrails = guardrailList(false);

      if (CollectionUtils.isNotEmpty(inputGuardrails) || CollectionUtils.isNotEmpty(outputGuardrails)) {
        GroupBuilder guardrailGroupBuilder = ui.group("Guardrails")
            .add(ui.label("Available guardrails:\n").create());
        
        if (CollectionUtils.isNotEmpty(inputGuardrails)) {
          guardrailGroupBuilder
              .add(ui.label(guardrailsHelp(true, inputGuardrails)).multiline().create())
              .add(ui.scriptField(Conf.INPUT_GUARD_RAILS).requireType(List.class).create());
        }
        
        if (CollectionUtils.isNotEmpty(outputGuardrails)) {
          guardrailGroupBuilder
              .add(ui.label(guardrailsHelp(false, outputGuardrails)).multiline().create())
              .add(ui.scriptField(Conf.OUTPUT_GUARD_RAILS).requireType(List.class).create());
        }
        guardrailGroupBuilder.add(ui.label("Select the guardrails to apply, or keep empty to use none").create())
            .create();
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

    private String guardrailsHelp(boolean isInput, List<String> guardrails) {
      String type = isInput ? "Input" : "Output";
      return String.format("%s guardrails:\n%s", type,
          guardrails.stream().map(g -> "- " + g).collect(Collectors.joining("\n")));
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
  }

  private static List<String> guardrailList(boolean isInput) {
    return isInput ? GuardrailFactory.providers().stream().map(SmartWorkflowInputGuardrail::name).collect(Collectors.toList()) : null;
  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/agent.png");
  }
}
