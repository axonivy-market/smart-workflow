package com.axonivy.utils.smart.workflow.program.internal;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailErrors;
import com.axonivy.utils.smart.workflow.memory.IvyMemory;
import com.axonivy.utils.smart.workflow.memory.id.IdStore;
import com.axonivy.utils.smart.workflow.memory.id.ProcessDataField;
import com.axonivy.utils.smart.workflow.memory.store.IvyVolatileStore;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.observability.AiListeners;
import com.axonivy.utils.smart.workflow.observability.AiListeners.AiProvider;
import com.axonivy.utils.smart.workflow.observability.AiListeners.ListenerCtxt;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.tools.human.internal.HumanInTheLoop;
import com.axonivy.utils.smart.workflow.tools.provider.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.router.ChatModelWrapper;
import dev.langchain4j.model.router.ModelRouter;
import dev.langchain4j.model.router.ModelRoutingStrategy;
import dev.langchain4j.service.AiServiceContext;
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

  interface Variable {
    String RESULT = "result";
  }

  public void execute() {
    Optional<UserMessage> query = QueryExpander.expandMacroWithFileExtraction(Conf.QUERY, context);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return; // early abort; user is still testing with empty values
    }

    var structured = execute(Conf.OUTPUT, Class.class);
    var aiCtxt = AiServiceContext.create(DynamicAgent.class);
    aiCtxt.returnType = structured.orElse(String.class);

    AiServices<DynamicAgent<?>> agentBuilder = AiServices.builder(aiCtxt);
    var memory = configureMemory(agentBuilder);
    var human = configureHumanInTheLoop(memory, agentBuilder);
    var toolFilter = context.config().getList(Conf.TOOLS);


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

  private record MemoryContext(IdStore memoryId, ChatMemoryStore store) {}

  private HumanInTheLoop configureHumanInTheLoop(MemoryContext memory, AiServices<? extends DynamicAgent<?>> agentBuilder) {
    HumanInTheLoop humanInTheLoop = new HumanInTheLoop(memory.memoryId, memory.store);
    agentBuilder.registerListeners(humanInTheLoop.provide());
    return humanInTheLoop;
  }

  private void configureModel(AiServices<? extends DynamicAgent<?>> agentBuilder, boolean structured, List<String> toolFilter) {
    var provider = ChatModelFactory.getProviderOrDefault(configuredProvider());
    var model = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);
    var agentName = context.element().name();
    var modelOptions = options()
        .modelName(model)
        .structuredOutput(structured)
        .hasTools(toolFilter != null && !toolFilter.isEmpty());
    var chatModel = provider.setup(modelOptions);

    var routed = ModelRouter.builder().addRoutes(chatModel)
    .defaultRoute(chatModel)
    .routingStrategy(new ModelRoutingStrategy() {
      @Override
      public ChatModelWrapper route(List<ChatModelWrapper> availableModels, ChatRequest chatRequest) {
        // any selection logic... 
        return availableModels.stream().findFirst()
          .orElseThrow(() -> new RuntimeException("No model available for request: " + chatRequest));
      }
    })
    .build();

    agentBuilder.chatModel(routed);
    var modelName = chatModel.defaultRequestParameters().modelName();
    AiListeners.create(new ListenerCtxt(new AiProvider(provider.name(), modelName), agentName))
        .forEach(agentBuilder::registerListener);
  }

  private String configuredProvider() {
    String providerName = null;
    List<String> providerConfig = context.config().getList(Conf.PROVIDER);
    if (!providerConfig.isEmpty()) {
      providerName = providerConfig.get(0);
      if (providerConfig.size() > 1) {
        Ivy.log().warn("Only one provider is allowed. Will use " + providerConfig.get(0) + ", and ignore other from: " + providerConfig);
      }
    }
    return providerName;
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
    var providers = GuardrailCollector.allProviders();
    var inputGuardrailFilters = context.config().getList(Conf.INPUT_GUARD_RAILS);
    agentBuilder.inputGuardrails(GuardrailCollector.inputGuardrailAdapters(providers, inputGuardrailFilters));
    var outputGuardrailFilters = context.config().getList(Conf.OUTPUT_GUARD_RAILS);
    agentBuilder.outputGuardrails(GuardrailCollector.outputGuardrailAdapters(providers, outputGuardrailFilters));
  }
}
