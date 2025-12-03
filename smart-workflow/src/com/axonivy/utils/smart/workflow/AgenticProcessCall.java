package com.axonivy.utils.smart.workflow;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.ui.AgentEditor;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.engine.IRequestId;
import ch.ivyteam.ivy.process.extension.impl.AbstractUserProcessExtension;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.process.model.diagram.icon.IconDecorator;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import dev.langchain4j.service.AiServices;

public class AgenticProcessCall extends AbstractUserProcessExtension implements IconDecorator {

  interface Variable {
    String RESULT = "result";
  }

  public interface Conf {
    String SYSTEM = "system";
    String QUERY = "query";
    String TOOLS = "tools";
    String MODEL = "model";
    String PROVIDER = "provider";
    String OUTPUT = "resultType";
    String MAP_TO = "resultMapping";
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
      new AgentEditor().initUiFields(ui);
    }
  }

  @Override
  public URI icon() {
    return URI.create("res:/webContent/logo/agent.png");
  }
}
