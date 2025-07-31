package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Abstract base class for AI functions that use JSON schemas for structured
 * data extraction.
 * 
 * This class provides a template method pattern for AI functions that: -
 * Generate JSON schemas based on target data structures - Call AI services with
 * structured output requirements - Parse JSON responses into domain objects
 * 
 * @param <T> The input data type (e.g., AiVariable, IvyTool, Object)
 * @param <R> The result type (e.g., List<AiVariable>, List<AiStep>, AiVariable)
 */
public abstract class AiFunction<T, R> {

  // AI service connector to handle communication with AI provider
  private AbstractAiServiceConnector connector;

  // The input query/prompt
  private String query;

  // Custom instructions to modify the function's behavior
  private List<String> customInstructions;

  // Input data for processing
  private List<T> inputData;

  // Execution result
  private R result;

  /**
   * Template method that defines the execution flow. Subclasses should not
   * override this method.
   */
  public final R execute() {
    try {
      // Step 1: Validate inputs
      if (!validateInputs()) {
        return handleValidationFailure();
      }

      // Step 2: Build template parameters
      Map<String, Object> parameters = buildParameters();

      // Step 3: Generate JSON schema
      JsonSchema jsonSchema = generateJsonSchema();
      if (jsonSchema == null) {
        return handleSchemaGenerationFailure();
      }

      // Step 4: Call AI service
      String aiResponse = callAiService(jsonSchema, parameters);
      if (StringUtils.isBlank(aiResponse)) {
        return handleAiServiceFailure();
      }

      // Step 5: Parse JSON response
      R parsedResult = parseJsonResponse(aiResponse);

      // Step 6: Process and validate result
      result = processResult(parsedResult);

      return result;

    } catch (Exception e) {
      return handleExecutionException(e);
    }
  }

  /**
   * Validates inputs before execution. Default implementation checks for
   * connector and query.
   */
  protected boolean validateInputs() {
    return connector != null && StringUtils.isNotBlank(query);
  }

  /**
   * Builds template parameters for the AI prompt. Subclasses must implement this
   * to provide domain-specific parameters.
   */
  protected abstract Map<String, Object> buildParameters();

  /**
   * Generates the JSON schema for structured AI output. Subclasses must implement
   * this to define their expected data structure.
   */
  protected abstract JsonSchema generateJsonSchema();

  /**
   * Parses the JSON response from the AI service. Subclasses must implement this
   * to convert JSON to their domain objects.
   */
  protected abstract R parseJsonResponse(String jsonResponse);

  /**
   * Processes the parsed result and performs any final validation or
   * transformation. Default implementation returns the parsed result as-is.
   */
  protected R processResult(R parsedResult) {
    return parsedResult;
  }

  /**
   * Returns the template string for the AI prompt. Subclasses must provide their
   * specific template.
   */
  protected abstract String getTemplate();

  /**
   * Calls the AI service with the generated schema and parameters.
   */
  protected final String callAiService(JsonSchema jsonSchema, Map<String, Object> parameters) {
    return connector.generateJson(jsonSchema, parameters, getTemplate());
  }

  /**
   * Formats custom instructions for inclusion in prompts.
   */
  protected final String formatCustomInstructions() {
    if (CollectionUtils.isEmpty(customInstructions)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    for (String instruction : customInstructions) {
      builder.append("- ").append(instruction).append(System.lineSeparator());
    }

    return builder.toString().strip();
  }

  /**
   * Safely parses JSON arrays from AI responses. Common utility for extracting
   * array data from AI JSON responses.
   */
  protected final List<String> parseJsonArray(String jsonResponse, String arrayFieldName) {
    List<String> result = new ArrayList<>();
    try {
      JsonNode arrayNode = BusinessEntityConverter.objectMapper.readTree(jsonResponse).get(arrayFieldName);

      if (arrayNode != null && arrayNode.isArray()) {
        for (JsonNode node : arrayNode) {
          result.add(node.asText());
        }
      }
    } catch (JsonProcessingException e) {
      // Return empty list on parsing failure
    }
    return result;
  }

  // JSON Schema Generation Utilities using langchain4j

  /**
   * Creates a JsonSchemaElement for a Java type using langchain4j's comprehensive
   * utilities. This handles primitives, collections, enums, complex objects, and
   * recursion automatically.
   */
  public static JsonSchemaElement createSchemaElementForType(Class<?> type) {
    return JsonSchemaElementUtils.jsonSchemaElementFrom(type);
  }

  /**
   * Builds a JsonObjectSchema from a Java class using langchain4j's sophisticated
   * utilities. This handles field detection, required fields, recursion, and
   * annotations automatically.
   */
  public static JsonObjectSchema buildObjectSchema(Class<?> clazz) {
    JsonSchemaElement element = JsonSchemaElementUtils.jsonSchemaElementFrom(clazz);
    if (element instanceof JsonObjectSchema) {
      return (JsonObjectSchema) element;
    }
    // Fallback: create empty object schema if the utility doesn't return an object
    // schema
    return JsonObjectSchema.builder().build();
  }

  /**
   * Adds description to a schema element based on field explanation. This method
   * preserves the original functionality for field explanations.
   */
  public static JsonSchemaElement addDescriptionToSchema(JsonSchemaElement schema, FieldExplanation explanation) {
    String description = explanation.getExplanation();

    if (schema instanceof JsonStringSchema) {
      return JsonStringSchema.builder().description(description).build();
    } else if (schema instanceof JsonIntegerSchema) {
      return JsonIntegerSchema.builder().description(description).build();
    } else if (schema instanceof JsonNumberSchema) {
      return JsonNumberSchema.builder().description(description).build();
    } else if (schema instanceof JsonBooleanSchema) {
      return JsonBooleanSchema.builder().description(description).build();
    } else if (schema instanceof JsonEnumSchema) {
      JsonEnumSchema enumSchema = (JsonEnumSchema) schema;
      return JsonEnumSchema.builder().description(description).enumValues(enumSchema.enumValues()).build();
    }

    return schema;
  }

  // Error handling methods that subclasses can override

  protected R handleValidationFailure() {
    Ivy.log().error("AiFunction validation failed");
    return null;
  }

  protected R handleSchemaGenerationFailure() {
    Ivy.log().error("AiFunction schema generation failed");
    return null;
  }

  protected R handleAiServiceFailure() {
    Ivy.log().error("AiFunction AI service call failed");
    return null;
  }

  protected R handleExecutionException(Exception e) {
    Ivy.log().error("AiFunction execution failed: " + e.getMessage());
    e.printStackTrace();
    return null;
  }

  // Getters and setters

  public AbstractAiServiceConnector getConnector() {
    return connector;
  }

  public void setConnector(AbstractAiServiceConnector connector) {
    this.connector = connector;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<String> getCustomInstructions() {
    return customInstructions;
  }

  public void setCustomInstructions(List<String> customInstructions) {
    this.customInstructions = customInstructions;
  }

  public List<T> getInputData() {
    return inputData;
  }

  public void setInputData(List<T> inputData) {
    this.inputData = inputData;
  }

  public R getResult() {
    return result;
  }

  /**
   * Abstract builder class for AiFunction implementations.
   * 
   * @param <T> The input data type that the AI function processes (e.g.,
   *            AiVariable, IvyTool, Object)
   * @param <R> The result type that the AI function returns (e.g.,
   *            List<AiVariable>, List<AiStep>, AiVariable)
   * @param <F> The concrete AI function type that extends AiFunction<T, R>
   */
  public abstract static class Builder<T, R, F extends AiFunction<T, R>> {
    protected AbstractAiServiceConnector connector;
    protected String query;
    protected List<String> customInstructions = new ArrayList<>();
    protected List<T> inputData = new ArrayList<>();

    public Builder<T, R, F> useService(AbstractAiServiceConnector connector) {
      this.connector = connector;
      return this;
    }

    public Builder<T, R, F> withQuery(String query) {
      this.query = query;
      return this;
    }

    public Builder<T, R, F> addCustomInstructions(List<String> instructions) {
      if (CollectionUtils.isNotEmpty(instructions)) {
        this.customInstructions.addAll(instructions);
      }
      return this;
    }

    public Builder<T, R, F> addCustomInstruction(String instruction) {
      if (StringUtils.isNotBlank(instruction)) {
        this.customInstructions.add(instruction);
      }
      return this;
    }

    public Builder<T, R, F> addInputData(List<T> data) {
      if (CollectionUtils.isNotEmpty(data)) {
        this.inputData.addAll(data);
      }
      return this;
    }

    public abstract F build();
  }
}