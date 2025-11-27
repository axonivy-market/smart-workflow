package com.axonivy.utils.smart.workflow;

import static com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions.options;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.AgenticProcessCall.Conf;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.output.DynamicAgent;
import com.axonivy.utils.smart.workflow.output.internal.StructuredOutputAgent;
import com.axonivy.utils.smart.workflow.tools.IvySubProcessToolsProvider;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.extension.exec.ProgramContext;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

class AgentExecutor {

  private final ProgramContext context;

  public AgentExecutor(ProgramContext context) {
    this.context = context;
  }

  public void chat() {
    var query = expand(Conf.QUERY);
    if (query.isEmpty()) {
      Ivy.log().info("Agent call was skipped, since there was no user query");
      return; // early abort; user is still testing with empty values
    }

    var agent = createAgent();
    var result = agent.chat(query.get());
    mapResult(result);

    Ivy.log().info("Agent response: " + result);
  }

  private void mapResult(Object result) {
    var mapTo = context.config().get(Conf.MAP_TO);
    if (mapTo != null) {
      String mapIt = mapTo + "=" + Variable.RESULT;
      try {
        context.script()
            .variable(Variable.RESULT, result)
            .executeScript(mapIt);
      } catch (Exception ex) {
        Ivy.log().error("Failed to map result to " + mapTo, ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private DynamicAgent<?> createAgent() {
    var structured = execute(Conf.OUTPUT, Class.class);
    Class<? extends DynamicAgent<?>> agentType = ChatAgent.class;
    if (structured.isPresent()) {
      agentType = StructuredOutputAgent.agent(structured.get());
    }

    var agentBuilder = AiServices.builder(agentType)
        .chatModel(model(structured.isPresent()))
        .toolProvider(tools());

    var systemMessage = expand(Conf.SYSTEM);
    if (systemMessage.isPresent()) {
      agentBuilder.systemMessageProvider(memId -> systemMessage.get());
    }

    return agentBuilder.build();
  }

  private ToolProvider tools() {
    @SuppressWarnings("unchecked")
    List<String> toolFilter = execute(Conf.TOOLS, List.class).orElse(null);
    return new IvySubProcessToolsProvider().filtering(toolFilter);
  }

  private ChatModel model(boolean isStructured) {
    String providerName = execute(Conf.PROVIDER, String.class).orElse(StringUtils.EMPTY);
    String modelName = execute(Conf.MODEL, String.class).orElse(StringUtils.EMPTY);
    var modelOptions = options()
        .modelName(modelName)
        .structuredOutput(isStructured);
    return ChatModelFactory.createModel(modelOptions, providerName);
  }

  private <T> Optional<T> execute(String configKey, Class<T> returnType) {
    return context.script().executeConfig(configKey, returnType);
  }

  private Optional<String> expand(String confKey) {
    try {
      @SuppressWarnings("restriction")
      var expanded = context.script().expandMacro(confKey);
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  interface ChatAgent extends DynamicAgent<String> {
    @Override
    String chat(String query);
  }

  interface Variable {
    String RESULT = "result";
  }

}
