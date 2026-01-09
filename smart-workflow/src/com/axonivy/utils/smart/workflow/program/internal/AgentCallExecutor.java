package com.axonivy.utils.smart.workflow.program.internal;

import static com.axonivy.utils.smart.workflow.guardrails.GuardrailProvider.USE_GUARDRAIL;
import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.GuardrailProvider;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.IvySubProcessToolsProvider;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import dev.langchain4j.service.AiServices;

public class AgentCallExecutor {

  private final ProgramContext context;

  public AgentCallExecutor(ProgramContext context) {
    this.context = context;
  }

  interface ChatAgent extends DynamicAgent<String> {
    @Override
    String chat(String query);
  }

  interface Variable {
    String RESULT = "result";
  }

  @SuppressWarnings("unchecked")
  public void execute() {
    var query = expand(Conf.QUERY);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return; // early abort; user is still testing with empty values
    }

    String providerName = execute(Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    String modelName = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);

    List<String> toolFilter = execute(Conf.TOOLS, List.class).orElse(null);
    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    var structured = execute(Conf.OUTPUT, Class.class);
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
    }
    var modelOptions = options()
        .modelName(modelName)
        .structuredOutput(structured.isPresent());
    var model = ChatModelFactory.createModel(modelOptions, providerName);

    var agentBuilder = AiServices.builder(agentType).chatModel(model)
        .toolProvider(new IvySubProcessToolsProvider().filtering(toolFilter));

    if (Boolean.parseBoolean(Ivy.var().get(USE_GUARDRAIL))) {
      List<String> guardraiFilters = execute(Conf.INPUT_GUARD_RAILS, List.class).orElse(null);
      List<InputGuardrailAdapter> inputGuardrails = GuardrailProvider.providersList(guardraiFilters);

      if (CollectionUtils.isNotEmpty(inputGuardrails)) {
        agentBuilder.inputGuardrails(inputGuardrails);
      }
    }

    var systemMessage = expand(Conf.SYSTEM);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(memId -> systemMessage.get());
    }

    var agent = agentBuilder.build();
    var result = agent.chat(query.get());

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

  private Optional<String> expand(String confKey) {
    try {
      var template = context.config().get(confKey);
      if (template == null || template.isBlank()) {
        return Optional.empty();
      }
      var expanded = context.script().expandMacro(template);
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

}
