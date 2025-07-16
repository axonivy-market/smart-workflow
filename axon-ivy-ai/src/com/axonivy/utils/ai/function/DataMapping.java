package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.enums.ExtractionStrategy;
import com.axonivy.utils.ai.function.strategy.InstructionBuilder;
import com.axonivy.utils.ai.function.strategy.InstructionsStrategy;
import com.axonivy.utils.ai.function.strategy.ResultProcessor;
import com.axonivy.utils.ai.function.strategy.WrapperStrategy;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;

public class DataMapping extends AiFunction {

  private static final String EXPLANATION_FORMAT = "\n   + %s : %s";

  private static final String EXPLANATION_TEMPLATE = """
      Field explanations:
         {{explanations}}
      """;

  // Object need to extract
  private Object targetObject;

  // The result is a list
  private Boolean asList;

  private List<FieldExplanation> fieldExplanations;

  // Extraction strategy (default to WRAPPER for backward compatibility)
  private ExtractionStrategy strategy = ExtractionStrategy.WRAPPER;

  // Strategy implementations
  private InstructionBuilder instructionBuilder;
  private ResultProcessor resultProcessor;

  // Retry configuration
  private int maxRetries = 3;
  private boolean retryEnabled = true;
  private ExtractionStrategy originalStrategy; // Remember user's chosen strategy
  private int currentAttempt = 0;

  public Object getTargetObject() {
    return this.targetObject;
  }

  public void setTargetObject(Object targetObject) {
    this.targetObject = targetObject;
  }

  public List<FieldExplanation> getFieldExplanations() {
    return this.fieldExplanations;
  }

  public void setFieldExplanations(List<FieldExplanation> fieldExplanations) {
    this.fieldExplanations = fieldExplanations;
  }

  public static Builder getBuilder() {
    return new Builder();
  }

  /**
   * Convert the targetObject to JSON string using Jackson
   * 
   * @return converted JSON object or empty if failed
   */
  public String convertObjectToJson() {
    try {
      return BusinessEntityConverter.getObjectMapper().writeValueAsString(targetObject);
    } catch (JsonProcessingException e) {
      return StringUtils.EMPTY;
    }
  }

  public String getFormattedFieldExplanations() {
    StringBuilder strBuilder = new StringBuilder();
    if (CollectionUtils.isEmpty(fieldExplanations)) {
      return StringUtils.EMPTY;
    }

    // Loop the field explanations and format them
    fieldExplanations.forEach(explanation -> {
      String formattedExplanation = String.format(EXPLANATION_FORMAT, explanation.getName(),
          explanation.getExplanation());
      strBuilder.append(formattedExplanation);
    });

    // Return the formatted explanations template
    Map<String, Object> params = new HashMap<>();
    params.put("explanations", strBuilder.toString().strip());
    return PromptTemplate.from(EXPLANATION_TEMPLATE).apply(params).text();
  }

  @Override
  protected void buildInstructions() {
    // Initialize strategies if not already done
    if (instructionBuilder == null) {
      initializeStrategies();
    }

    // Use the strategy to build instructions
    String strategyInstructions = instructionBuilder.buildInstructions(targetObject);

    // If the generated instruction is empty, it means something went wrong, set the
    // error variable to true
    if (StringUtils.isAllBlank(strategyInstructions)) {
      failedToBuildInstructions = true;
      return;
    }
    setFunctionInstructions(strategyInstructions);

    // Build the custom instructions (field explanations still apply to both
    // strategies)
    if (getCustomInstructions() == null) {
      setCustomInstructions(new ArrayList<>());
    }

    // Add field explanations if available
    String fieldExplanationsText = getFormattedFieldExplanations();
    if (StringUtils.isNotBlank(fieldExplanationsText)) {
      getCustomInstructions().add(fieldExplanationsText);
    }
  }

  @Override
  protected String standardizeResult(String result) {
    // Use the strategy to process the result
    return resultProcessor.processResult(result);
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

  public Boolean getAsList() {
    return asList;
  }

  public void setAsList(Boolean asList) {
    this.asList = asList;
    // Reinitialize strategies since they depend on the asList property
    if (strategy != null) {
      initializeStrategies();
    }
  }

  public ExtractionStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(ExtractionStrategy strategy) {
    this.strategy = strategy;
    initializeStrategies();
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public boolean isRetryEnabled() {
    return retryEnabled;
  }

  public void setRetryEnabled(boolean retryEnabled) {
    this.retryEnabled = retryEnabled;
  }

  public int getCurrentAttempt() {
    return currentAttempt;
  }

  /**
   * Initializes the strategy implementations based on the current strategy.
   */
  private void initializeStrategies() {
    switch (strategy) {
      case WRAPPER:
        this.instructionBuilder = new WrapperStrategy(asList);
        this.resultProcessor = new WrapperStrategy(asList);
        setUseWrappers(true);
        break;
      case INSTRUCTIONS:
        this.instructionBuilder = new InstructionsStrategy(asList);
        this.resultProcessor = new InstructionsStrategy(asList);
        setUseWrappers(false);
        break;
    }
  }

  /**
   * Gets the alternative strategy for fallback retry.
   */
  private ExtractionStrategy getAlternativeStrategy(ExtractionStrategy current) {
    return current == ExtractionStrategy.WRAPPER ? ExtractionStrategy.INSTRUCTIONS : ExtractionStrategy.WRAPPER;
  }

  /**
   * Determines if the result from AI is valid and successful by attempting to
   * parse it back to the target object.
   */
  private boolean isValidResult(AiVariable result) {
    if (result == null) {
      return false;
    }

    // Check if result state indicates success
    if (result.getState() != AiVariableState.SUCCESS) {
      return false;
    }

    // Check if content is not blank
    if (StringUtils.isBlank(result.getContent())) {
      return false;
    }

    // The ultimate validation: try to parse the result back to the target object
    if (targetObject != null) {
      try {
        if (asList != null && asList) {
          // For list results, try to parse as array
          BusinessEntityConverter.jsonValueToEntities(result.getContent(), targetObject.getClass());
        } else {
          // For single object results, try to parse as single object
          BusinessEntityConverter.jsonValueToEntity(result.getContent(), targetObject.getClass());
        }
        // If parsing succeeds, the result is valid
        return true;
      } catch (Exception e) {
        // If parsing fails, the result is not valid regardless of other checks
        System.out.println("Result validation failed - JSON parsing error: " + e.getMessage());
        return false;
      }
    }

    // Fallback: basic validation if no target object is available
    String content = result.getContent().toLowerCase();
    if (content.contains("error") || content.contains("failed") || content.contains("invalid")) {
      return false;
    }

    return true;
  }

  /**
   * Executes the data mapping with retry logic and strategy fallback.
   * 
   * Retry strategy: - Attempt 1-2: Use user's chosen strategy - Attempt 3: Switch
   * to alternative strategy as fallback - If all attempts fail: Return error
   * result
   */
  @Override
  public AiVariable execute() {
    failedToBuildInstructions = false;

    // If retry is disabled, use the parent's execute method directly
    if (!retryEnabled) {
      return super.execute();
    }

    // Remember the original strategy chosen by user
    originalStrategy = strategy;
    AiVariable lastResult = null;

    for (currentAttempt = 1; currentAttempt <= maxRetries; currentAttempt++) {
      try {
        // Log current attempt and strategy
        Ivy.log()
            .info(String.format("DataMapping attempt %d/%d using %s strategy", currentAttempt, maxRetries, strategy));

        // On the last attempt, switch to alternative strategy if available
        if (currentAttempt == maxRetries && maxRetries > 2) {
          ExtractionStrategy alternativeStrategy = getAlternativeStrategy(originalStrategy);
          if (alternativeStrategy != originalStrategy) {
            Ivy.log().info(String.format("Switching to fallback strategy: %s", alternativeStrategy));
            setStrategy(alternativeStrategy);
          }
        }

        // Execute using parent's logic
        AiVariable result = super.execute();
        lastResult = result;

        // Check if result is valid
        if (isValidResult(result)) {
          Ivy.log().info(String.format("Success on attempt %d with %s strategy", currentAttempt, strategy));
          resetToOriginalStrategy();
          return result;
        }

        // Log failure and continue to next attempt
        String errorReason = result != null ? String.format("Invalid result - State: %s, Content: %s",
            result.getState(), StringUtils.abbreviate(result.getContent(), 50)) : "Null result";
        Ivy.log().info(String.format("Attempt %d failed: %s", currentAttempt, errorReason));

      } catch (Exception e) {
        Ivy.log().error(String.format("Attempt %d threw exception: %s", currentAttempt, e.getMessage()));
        lastResult = buildErrorResult();
      }
    }

    // All attempts failed
    Ivy.log().error(String.format("All %d attempts failed. Returning error result.", maxRetries));
    resetToOriginalStrategy();

    // Return the last result or a generic error if no result was obtained
    return lastResult != null ? lastResult : buildErrorResult();
  }

  /**
   * Resets the strategy back to the original user-chosen strategy.
   */
  private void resetToOriginalStrategy() {
    if (originalStrategy != null && originalStrategy != strategy) {
      setStrategy(originalStrategy);
    }
    currentAttempt = 0;
  }

  // Builder class for DataMapper
  public static class Builder extends AiFunction.Builder {
    private Object targetObject;
    private Boolean asList;
    private List<FieldExplanation> fieldExplanations;
    private ExtractionStrategy strategy = ExtractionStrategy.WRAPPER; // Default to wrapper

    // Retry configuration
    private int maxRetries = 3;
    private boolean retryEnabled = true;

    public Builder asList(Boolean asList) {
      this.asList = asList;
      return this;
    }

    public Builder addFieldExplanations(List<FieldExplanation> fieldExplanations) {
      // Initialize `fieldExplanations if necessary
      if (this.fieldExplanations == null) {
        this.fieldExplanations = new ArrayList<>();
      }

      // Add explanation to the Map
      this.fieldExplanations.addAll(fieldExplanations);
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
        this.customInstructions.addAll(instructions);
      }
      return this;
    }

    public Builder addExamples(List<AiExample> examples) {
      if (CollectionUtils.isNotEmpty(examples)) {
        this.examples.addAll(examples);
      }
      return this;
    }

    public Builder withStrategy(ExtractionStrategy strategy) {
      this.strategy = strategy;
      return this;
    }

    public Builder withInstructionsStrategy() {
      this.strategy = ExtractionStrategy.INSTRUCTIONS;
      return this;
    }

    public Builder withWrapperStrategy() {
      this.strategy = ExtractionStrategy.WRAPPER;
      return this;
    }

    public Builder withJsonSchema(Class<?> schemaClass) {
      try {
        // Create an instance of the class to generate schema from
        this.targetObject = schemaClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        // If instantiation fails, keep the current target object
      }
      return this;
    }

    public Builder withMaxRetries(int maxRetries) {
      this.maxRetries = Math.max(1, maxRetries); // Ensure at least 1 retry
      return this;
    }

    public Builder enableRetry() {
      this.retryEnabled = true;
      return this;
    }

    public Builder disableRetry() {
      this.retryEnabled = false;
      return this;
    }

    public Builder withRetryEnabled(boolean retryEnabled) {
      this.retryEnabled = retryEnabled;
      return this;
    }

    public Builder withObject(Object object) {
      this.targetObject = object;
      return this;
    }

    public Builder withTargetObject(Object targetObject) {
      this.targetObject = targetObject;
      return this;
    }

    public Builder withTargetJson(String json) {
      try {
        this.targetObject = BusinessEntityConverter.jsonValueToEntity(json, Object.class);
      } catch (Exception e) {
        this.targetObject = null;
      }
      return this;
    }

    @Override
    public DataMapping build() {
      DataMapping dataMapper = new DataMapping();
      dataMapper.setConnector(connector);
      dataMapper.setTargetObject(targetObject);
      dataMapper.setAsList(asList);
      dataMapper.setQuery(query);
      dataMapper.setCustomInstructions(customInstructions);
      dataMapper.setExamples(examples);
      dataMapper.setFieldExplanations(fieldExplanations);
      dataMapper.setStrategy(strategy);

      // Set retry configuration
      dataMapper.setMaxRetries(maxRetries);
      dataMapper.setRetryEnabled(retryEnabled);

      return dataMapper;
    }
  }
}