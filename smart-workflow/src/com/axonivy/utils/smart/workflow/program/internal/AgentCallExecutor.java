package com.axonivy.utils.smart.workflow.program.internal;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailErrors;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;
import com.axonivy.utils.smart.workflow.memory.IvyMemory;
import com.axonivy.utils.smart.workflow.memory.id.IdStore;
import com.axonivy.utils.smart.workflow.memory.id.ProcessDataField;
import com.axonivy.utils.smart.workflow.memory.store.IvyVolatileStore;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.observability.AiListeners;
import com.axonivy.utils.smart.workflow.observability.AiListeners.AiProvider;
import com.axonivy.utils.smart.workflow.observability.AiListeners.ListenerCtxt;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.human.internal.HumanInTheLoop;
import com.axonivy.utils.smart.workflow.tools.provider.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

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
    var memory = configureMemory(agentBuilder);
    var human = configureHumanInTheLoop(memory, agentBuilder);
    var toolFilter = executeListOfStrings(Conf.TOOLS).orElse(null);
    configureModel(agentBuilder, structured.isPresent(), toolFilter);
    configureToolProvider(agentBuilder, toolFilter);
    configureGuardrails(agentBuilder);
    configureSystemMessage(human, agentBuilder);
    var agent = agentBuilder.build();

    try {
      List<Content> contents = human.userMessage(query.get().contents());
      Object result = agent.chat(contents);
      var mapTo = context.config().get(Conf.MAP_TO);
      if (mapTo != null) {
        String mapIt = mapTo + "=result";
        try {
          context.script().variable(Variable.RESULT, result).executeScript(mapIt);
        } catch (Exception ex) {
          Ivy.log().error("Failed to map result to " + mapTo, ex);
        }
      }
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

  private void configureSystemMessage(HumanInTheLoop human, AiServices<? extends DynamicAgent<?>> agentBuilder) {
    if (human.isRestoredConversion()) {
      return; // keep system message from initial conversion
    }
    var systemMessage = QueryExpander.expandMacro(Conf.SYSTEM, context);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(_ -> systemMessage.get());
    }
  }

  private MemoryContext configureMemory(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    var store = new IvyVolatileStore();
    var memory = new IvyMemory(ChatMemoryService.DEFAULT, store);
    agentBuilder.chatMemory(memory);
    return new MemoryContext(new ProcessDataField(context.script()), store);
  }

  private record MemoryContext(IdStore memoryId, ChatMemoryStore store) { }

  private HumanInTheLoop configureHumanInTheLoop(MemoryContext memory, AiServices<? extends DynamicAgent<?>> agentBuilder) {
    HumanInTheLoop humanInTheLoop = new HumanInTheLoop(memory.memoryId, memory.store);
    agentBuilder.registerListeners(humanInTheLoop.provide());
    return humanInTheLoop;
  }

  private void configureModel(AiServices<? extends DynamicAgent<?>> agentBuilder, boolean structured, List<String> toolFilter) {
    var providerName = execute(Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    var model = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);
    var provider = ChatModelFactory.getProviderOrDefault(providerName);
    var modelOptions = options()
        .modelName(model)
        .structuredOutput(structured)
        .hasTools(toolFilter != null && !toolFilter.isEmpty());
    var chatModel = provider.setup(modelOptions);
    agentBuilder.chatModel(chatModel);
    var modelName = chatModel.defaultRequestParameters().modelName();
    AiListeners.create(new ListenerCtxt(new AiProvider(provider.name(), modelName)))
      .forEach(agentBuilder::registerListener);
  }

  private void configureToolProvider(AiServices<? extends DynamicAgent<?>> agentBuilder, List<String> toolFilter) {
    ToolProvider ivyTools = new IvySubProcessToolsProvider().filtering(toolFilter);
    agentBuilder.toolProvider(request -> {
      List<AiServiceTool> all = new ArrayList<>(ivyTools.provideTools(request).aiServiceTools());
      all.addAll(SmartWorkflowToolsProvider.provideTools(toolFilter).aiServiceTools());
      return new ToolProviderResult(all);
    });
    agentBuilder.toolExecutionErrorHandler(new IvyToolErrorHandler());
  }

  private void configureGuardrails(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    Set<GuardrailProvider> providers = GuardrailCollector.allProviders();
    List<String> inputGuardrailFilters = executeListOfStrings(Conf.INPUT_GUARD_RAILS).orElse(null);
    agentBuilder.inputGuardrails(GuardrailCollector.inputGuardrailAdapters(providers, inputGuardrailFilters));
    List<String> outputGuardrailFilters = executeListOfStrings(Conf.OUTPUT_GUARD_RAILS).orElse(null);
    agentBuilder.outputGuardrails(GuardrailCollector.outputGuardrailAdapters(providers, outputGuardrailFilters));
  }
}
