package com.axonivy.utils.smart.workflow.program.internal;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.memory.SmartWorkflowChatMemoryStore;
import com.axonivy.utils.smart.workflow.governance.memory.SmartWorkflowChatModelListener;
import com.axonivy.utils.smart.workflow.guardrails.GuardrailCollector;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import ch.ivyteam.ivy.workflow.ICase;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

public class AgentCallExecutor {
  private static final String GUARDRAIL_ERROR_CODE = "smartworkflow:guardrail:violation";

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
    var  listener = configureModel(agentBuilder, structured.isPresent());

    configureToolProvider(agentBuilder);
    configureInputGuardrails(agentBuilder);
    configureSystemMessage(agentBuilder);
    configureMemory(agentBuilder, listener);

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
    } catch (InputGuardrailException ex) {
      throwGuardrailError(ex);
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
      agentBuilder.systemMessageProvider(memId -> systemMessage.get());
    }
  }

  private SmartWorkflowChatModelListener configureModel(AiServices<? extends DynamicAgent<?>> agentBuilder, boolean isStructured) {
    String providerName = execute(Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    String modelName = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);
    var modelOptions = options()
        .modelName(modelName)
        .structuredOutput(isStructured);
    var rawModel = ChatModelFactory.createModel(modelOptions, providerName);
    var listener = new SmartWorkflowChatModelListener();
    agentBuilder.chatModel(new ListeningChatModel(rawModel, listener));
    return listener;
  }

  private void configureToolProvider(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    List<String> toolFilter = executeListOfStrings(Conf.TOOLS).orElse(null);
    agentBuilder.toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter));
  }

  private void configureInputGuardrails(AiServices<? extends DynamicAgent<?>> agentBuilder) {
    List<String> guardrailFilters = executeListOfStrings(Conf.INPUT_GUARD_RAILS).orElse(null);
    List<InputGuardrailAdapter> inputGuardrails = GuardrailCollector.inputGuardrailAdapters(guardrailFilters);

    if (CollectionUtils.isNotEmpty(inputGuardrails)) {
      agentBuilder.inputGuardrails(inputGuardrails);
    }
  }

  private void configureMemory(AiServices<? extends DynamicAgent<?>> agentBuilder, SmartWorkflowChatModelListener listener) {
    var agentProfileId = execute(Conf.AGENT_PROFILE, String.class);

    if (agentProfileId.isPresent()) {
      String caseUuid = Optional.ofNullable(Ivy.wfCase()).map(ICase::uuid).orElse(null);
      agentBuilder.chatMemory(
          MessageWindowChatMemory.builder()
              .id(IdGenerationUtils.generateRandomId())
              .maxMessages(100)
              .chatMemoryStore(new SmartWorkflowChatMemoryStore(agentProfileId.get(), listener, caseUuid))
              .build());
    }
  }

  private void throwGuardrailError(InputGuardrailException ex) {
    BpmPublicErrorBuilder errorBuilder = BpmError.create(GUARDRAIL_ERROR_CODE);
    Optional.ofNullable(ex.getMessage()).ifPresent(message -> errorBuilder.withMessage(message));
    Optional.ofNullable(ex.getCause()).ifPresent(cause -> errorBuilder.withCause(ex));
    errorBuilder.throwError();
  }
}
