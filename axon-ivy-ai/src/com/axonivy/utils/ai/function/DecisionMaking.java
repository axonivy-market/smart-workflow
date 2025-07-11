package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiOption;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;

import dev.langchain4j.model.input.PromptTemplate;

public class DecisionMaking extends AiFunction {

  private static final String INSTRUCTION_TEMPLATE = """
      Options:
          {{options}}

      - an option is an ID - condition pair
      - analyze the query, choose the most suitable option
      - the result MUST be the ID of the chosen option
      """;

  private static final String OPTION_FORMAT = """
      - ID: %s
        condition: %s

      """;

  // List of options
  private List<AiOption> options;

  public List<AiOption> getOptions() {
    return this.options;
  }

  public static Builder getBuilder() {
    return new Builder();
  }

  public void setOptions(List<AiOption> options) {
    this.options = options;
  }

  @Override
  protected void buildInstructions() {
    // If there is no decision to make, just return the empty instruction
    if (options == null || options.isEmpty()) {
      setFunctionInstructions(StringUtils.EMPTY);
      return;
    }

    // Format the decisions to an easy-to-read format
    // It help AI understand better
    Map<String, Object> params = new HashMap<>();
    params.put("options", formatOptions());
    setFunctionInstructions(PromptTemplate.from(INSTRUCTION_TEMPLATE).apply(params).text());
  }

  @Override
  protected AiVariable createStandardResult(String resultFromAI) {
    if (StringUtils.isBlank(resultFromAI)) {
      return buildErrorResult();
    }
    AiVariable result = new AiVariable();
    result.init();
    result.setContent(resultFromAI);
    result.setState(AiVariableState.SUCCESS);
    return result;
  }

  private String formatOptions() {
    String result = StringUtils.EMPTY;
    for (var option : options) {
      // Build line of option
      String line = String.format(OPTION_FORMAT, option.getId(), option.getCondition());

      // Append to the result
      result = result.concat(line);
    }
    return result;
  }

  // Builder class for DecisionMaker
  public static class Builder extends AiFunction.Builder {
    private List<AiOption> options = new ArrayList<>();

    public Builder addOptions(List<AiOption> options) {
      this.options.addAll(options);
      return this;
    }

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

    public Builder addCustomInstructions(List<String> instructions) {
      if (CollectionUtils.isNotEmpty(instructions)) {
        this.customInstructions.addAll(instructions.stream().filter(instruction -> StringUtils.isNotBlank(instruction))
            .collect(Collectors.toList()));
      }
      return this;
    }

    public Builder addExamples(List<AiExample> examples) {
      if (CollectionUtils.isNotEmpty(examples)) {
        this.examples.addAll(examples);
      }
      return this;
    }

    @Override
    public DecisionMaking build() {
      DecisionMaking decisionMaking = new DecisionMaking();
      decisionMaking.setConnector(connector);
      decisionMaking.setQuery(query);
      decisionMaking.setOptions(options);
      decisionMaking.setExamples(examples);
      decisionMaking.setCustomInstructions(customInstructions);
      return decisionMaking;
    }
  }
}