package com.axonivy.utils.smart.workflow.program.internal;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.listener.ChatHistoryListener;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailErrors;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.provider.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;

public class AgentCallExecutor {

  private final ProgramContext context;

  public AgentCallExecutor(ProgramContext context) {
    this.context = context;
  }

  interface ChatAgent extends DynamicAgent<String> {
    @Override
    String chat(List<Content> query);
  }

  interface Variable {
    String RESULT = "result";
  }

  @SuppressWarnings("unchecked")
  public void execute() {
    Optional<UserMessage> query = QueryExpander.expandMacroWithFileExtraction(Conf.QUERY, context);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return; // early abort; user is still testing with empty values
    }

    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    var structured = execute(Conf.OUTPUT, Class.class);
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
    }

    var agentBuilder = AiServices.builder(agentType);
    configureModel(agentBuilder, structured.isPresent());

    configureToolProvider(agentBuilder);
    configureGuardrails(agentBuilder);
    configureSystemMessage(agentBuilder);

    var agent = agentBuilder.build();
    try {
      Object result = agent.chat(query.get().contents());
      var mapTo = context.config().get(Conf.MAP_TO);
      if (mapTo != null) {
        String mapIt = mapTo + "=result";
        try {
          context.script().variable(Variable.RESULT, result).executeScript(mapIt);
        } catch (Exception ex) {
          Ivy.log().error("Failed to map result to " + mapTo, ex);
        }
      }
      Ivy.log().info("Agent response: " + result);
    } catch (InputGuardrailException | OutputGuardrailException ex) {
      GuardrailErrors.throwError(ex);
    }
  }

  private <T> Optional<T> execute(String configKey, Class<T> returnType) {
    var value = Optional.ofNullable(context.config().get(configKey))
        .filter(Predicate.not(String::isBlank));
    if (value.isEmpty()) {
      return Optional.empty();
    }
    try {
      return context.script().executeExpression(value.get(), returnType);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to extract config '" + configKey + "' for value '" + value.get() + "'",
          ex);
    }
  }

  private Optional<List<String>> executeListOfStrings(String configKey) {
    return execute(configKey, List.class)
        .map(rawList -> ((List<?>) rawList).stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList());
  }

  private void configureSystemMessage(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    var systemMessage = QueryExpander.expandMacro(Conf.SYSTEM, context);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(_ -> systemMessage.get());
    }
  }

  private void configureModel(AiServices<? extends DynamicAgent<?>> agentBuilder, boolean structured) {
    var providerName = execute(Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    var model = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);
    var provider = ChatModelFactory.getProviderOrDefault(providerName);
    var modelOptions = options()
        .modelName(model)
        .structuredOutput(structured);
    var chatModel = provider.setup(modelOptions);
    agentBuilder.chatModel(chatModel);
    var modelName = chatModel.defaultRequestParameters().modelName();
    new ChatHistoryListener().configure().forEach(agentBuilder::registerListener);
    new OpenInferenceTracing(provider.name(), modelName).configure().forEach(agentBuilder::registerListener);
  }

  private void configureToolProvider(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    List<String> toolFilter = executeListOfStrings(Conf.TOOLS).orElse(null);
    ToolProvider ivyTools = new IvySubProcessToolsProvider().filtering(toolFilter);
    agentBuilder.toolProvider(request -> {
      Map<ToolSpecification, ToolExecutor> all = new HashMap<>(ivyTools.provideTools(request).tools());
      all.putAll(SmartWorkflowToolsProvider.provideTools(toolFilter).tools());
      return new ToolProviderResult(all);
    });
  }

  private void configureGuardrails(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    List<String> inputGuardrailFilters = executeListOfStrings(Conf.INPUT_GUARD_RAILS).orElse(null);
    agentBuilder.inputGuardrails(GuardrailCollector.inputGuardrailAdapters(inputGuardrailFilters));
    List<String> outputGuardrailFilters = executeListOfStrings(Conf.OUTPUT_GUARD_RAILS).orElse(null);
    agentBuilder.outputGuardrails(GuardrailCollector.outputGuardrailAdapters(outputGuardrailFilters));
  }
}
