package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiOption;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

public class DecisionMaker extends AiFunction<AiOption, AiVariable> {

  private static final String TEMPLATE = """
      QUERY:
      {{query}}
      -------------------------------
      OPTIONS:
      {{options}}
      -------------------------------
      INSTRUCTIONS:
      - An option is an ID - condition pair
      - Analyze the query carefully and choose the most suitable option
      - The result MUST be the ID of the chosen option
      {{customInstructions}}
      -------------------------------
      """;

  private static final String OPTION_FORMAT = """
      - ID: %s
        Condition: %s

      """;

  private static final String SELECTED_OPTION_ID = "selectedOptionId";

  // List of options
  private List<AiOption> options;

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected boolean validateInputs() {
    return super.validateInputs() && CollectionUtils.isNotEmpty(options);
  }

  @Override
  protected Map<String, Object> buildParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", getQuery());
    params.put("options", formatOptions());
    params.put("customInstructions", formatCustomInstructions());
    return params;
  }

  @Override
  protected JsonSchema generateJsonSchema() {
    JsonSchemaElement selectedOptionSchema = JsonStringSchema.builder()
        .description("The ID of the selected option that best matches the query").build();

    JsonSchemaElement rootSchema = JsonObjectSchema.builder().description("The result of the decision making process")
        .addProperty(SELECTED_OPTION_ID, selectedOptionSchema).required(SELECTED_OPTION_ID).build();

    return JsonSchema.builder().name("DecisionResult").rootElement(rootSchema).build();
  }

  @Override
  protected AiVariable parseJsonResponse(String jsonResponse) {
    try {
      JsonNode rootNode = BusinessEntityConverter.objectMapper.readTree(jsonResponse);
      JsonNode selectedIdNode = rootNode.get(SELECTED_OPTION_ID);

      if (selectedIdNode != null && !selectedIdNode.isNull()) {
        String selectedOptionId = selectedIdNode.asText();

        // Validate that the selected option ID exists in our options
        boolean validOption = options.stream().anyMatch(option -> option.getId().equals(selectedOptionId));

        if (validOption) {
          AiVariable result = new AiVariable();
          result.init();
          result.getParameter().setValue(selectedOptionId);
          result.getParameter().setClassName("String");
          result.setState(AiVariableState.SUCCESS);
          return result;
        }
      }
    } catch (Exception e) {
      // Fall through to error handling
    }

    return buildErrorResult();
  }

  @Override
  protected String getTemplate() {
    return TEMPLATE;
  }

  @Override
  protected AiVariable handleValidationFailure() {
    return buildErrorResult();
  }

  @Override
  protected AiVariable handleSchemaGenerationFailure() {
    return buildErrorResult();
  }

  @Override
  protected AiVariable handleAiServiceFailure() {
    return buildErrorResult();
  }

  @Override
  protected AiVariable handleExecutionException(Exception e) {
    return buildErrorResult();
  }

  private AiVariable buildErrorResult() {
    AiVariable result = new AiVariable();
    result.init();
    result.getParameter().setValue("ERROR");
    result.getParameter().setClassName("String");
    result.setState(AiVariableState.ERROR);
    return result;
  }

  private String formatOptions() {
    if (CollectionUtils.isEmpty(options)) {
      return StringUtils.EMPTY;
    }

    StringBuilder result = new StringBuilder();
    for (AiOption option : options) {
      String line = String.format(OPTION_FORMAT, option.getId(), option.getCondition());
      result.append(line);
    }
    return result.toString();
  }

  // Getters and setters for domain-specific fields
  public List<AiOption> getOptions() {
    return options;
  }

  public void setOptions(List<AiOption> options) {
    this.options = options;
  }

  // Builder class for DecisionMaking
  public static class Builder extends AiFunction.Builder<AiOption, AiVariable, DecisionMaker> {
    private List<AiOption> options = new ArrayList<>();

    @Override
    public Builder useService(AbstractAiServiceConnector connector) {
      return (Builder) super.useService(connector);
    }

    @Override
    public Builder withQuery(String query) {
      return (Builder) super.withQuery(query);
    }

    @Override
    public Builder addCustomInstructions(List<String> instructions) {
      return (Builder) super.addCustomInstructions(instructions);
    }

    @Override
    public Builder addCustomInstruction(String instruction) {
      return (Builder) super.addCustomInstruction(instruction);
    }

    @Override
    public Builder addInputData(List<AiOption> data) {
      return (Builder) super.addInputData(data);
    }

    public Builder addOptions(List<AiOption> options) {
      if (CollectionUtils.isNotEmpty(options)) {
        this.options.addAll(options);
      }
      return this;
    }

    public Builder addOption(AiOption option) {
      if (option != null) {
        this.options.add(option);
      }
      return this;
    }

    @Override
    public DecisionMaker build() {
      DecisionMaker decisionMaking = new DecisionMaker();
      decisionMaking.setConnector(connector);
      decisionMaking.setQuery(query);
      decisionMaking.setCustomInstructions(customInstructions);
      decisionMaking.setOptions(options);
      return decisionMaking;
    }
  }
}