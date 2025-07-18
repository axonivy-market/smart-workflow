package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;

import dev.langchain4j.model.input.PromptTemplate;

public class Planning extends AiFunction {
  private static final String INSTRUCTION_TEMPLATE = """
      {{agents}}
      - Each agent is capable of doing some tasks, analyze carefully before making decision
      - Only use the agents above, create a step-by-step plan to handle the query
      - Each step of the plan should have three info:
        + step number (an incremental number field, start with 1)
        + explanation why use this agent
        + agent ID: ID of the selected agent
      """;

  private static final String AGENT_PART_TEMPLATE = """
      Avaiable agents:
      {{agents}}
      """;

  private static final String TOOL_LINE_FORMAT = """
        + ID: %s
          Usage: %s
      """;

  private List<IvyTool> tools;

  public List<IvyTool> getTools() {
    return tools;
  }

  public void setTools(List<IvyTool> tools) {
    this.tools = tools;
  }

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected void buildInstructions() {
    String prompt = CollectionUtils.isEmpty(tools) ? StringUtils.EMPTY : buildPrompt();

    // Format the decisions to an easy-to-read format
    // It help AI understand better
    Map<String, Object> params = new HashMap<>();
    params.put("agents", prompt);
    setFunctionInstructions(PromptTemplate.from(INSTRUCTION_TEMPLATE).apply(params).text().strip());
  }

  @Override
  protected AiVariable createStandardResult(String resultFromAI) {
    if (StringUtils.isBlank(resultFromAI)) {
      return buildErrorResult();
    }
    AiVariable result = new AiVariable();
    result.init();
    result.getParameter().setValue(resultFromAI);
    result.getParameter().setClassName("String");
    result.setState(AiVariableState.SUCCESS);
    return result;
  }

  private String buildPrompt() {
    String result = StringUtils.EMPTY;
    for (var tool : tools) {
      // Format agents
      String line = String.format(TOOL_LINE_FORMAT, tool.getId(), tool.getUsage());

      // Append to the result
      result = result.concat(line);
    }

    // Format the agent prompt
    Map<String, Object> params = new HashMap<>();
    params.put("agents", result);
    return PromptTemplate.from(AGENT_PART_TEMPLATE).apply(params).text();
  }

  // Builder class for Planning
  public static class Builder extends AiFunction.Builder {
    private List<IvyTool> tools;

    public Builder useService(AbstractAiServiceConnector connector) {
      this.connector = connector;
      return this;
    }

    public Builder withQuery(String query) {
      this.query = query;
      return this;
    }

    public Builder addCustomInstruction(String instruction) {
      if (StringUtils.isNotBlank(instruction)) {
        this.customInstructions.add(instruction);
      }
      return this;
    }

    public Builder addExamples(List<AiExample> examples) {
      if (StringUtils.isNotBlank(query)) {
        this.examples.addAll(examples);
      }
      return this;
    }

    public Builder addTool(IvyTool tool) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }
      this.tools.add(tool);
      return this;
    }

    public Builder addTools(List<IvyTool> tools) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }
      this.tools.addAll(tools);
      return this;
    }

    @Override
    public Planning build() {
      Planning planning = new Planning();
      planning.setConnector(connector);
      planning.setQuery(query);
      planning.setTools(tools);
      planning.setCustomInstructions(customInstructions);
      return planning;
    }
  }
}
