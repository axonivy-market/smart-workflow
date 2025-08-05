package com.axonivy.utils.ai.function;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

public class DataMapper extends AiFunction<Object, Object> {

  private static final String TEMPLATE = """
      QUERY:
      {{query}}
      -------------------------------
      INSTRUCTIONS:
      {{customInstructions}}
      """;

  // Object need to extract
  private Object targetObject;

  // The result is a list
  private Boolean asList;

  private List<FieldExplanation> fieldExplanations;

  public static Builder getBuilder() {
    return new Builder();
  }

  @Override
  protected boolean validateInputs() {
    return super.validateInputs() && targetObject != null;
  }

  @Override
  protected Map<String, Object> buildParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", getQuery());
    params.put("customInstructions", formatCustomInstructions());
    return params;
  }

  @Override
  protected JsonSchema generateJsonSchema() {
    if (targetObject == null) {
      return null;
    }

    String schemaName = targetObject.getClass().getSimpleName();
    JsonObjectSchema rootSchema = buildObjectSchemaWithExplanations(targetObject.getClass());

    if (rootSchema == null) {
      return null;
    }

    return JsonSchema.builder().name(schemaName).rootElement(rootSchema).build();
  }

  @Override
  protected Object parseJsonResponse(String jsonResponse) {
    if (StringUtils.isBlank(jsonResponse)) {
      return buildErrorResult();
    }

    
    // Put the parsed object get from AI to the result
    // If failed, put the JSON object instead
    try {
      return BusinessEntityConverter.jsonValueToEntity(jsonResponse, targetObject.getClass());
    } catch (Exception e) {
      return jsonResponse;
    }
  }

  @Override
  protected Object processResult(Object parsedResult) {
    // Validate the result using the original validation logic
    if (isValidResult(parsedResult)) {
      return parsedResult;
    }

    return buildErrorResult();
  }

  @Override
  protected String getTemplate() {
    return TEMPLATE;
  }

  @Override
  protected Object handleValidationFailure() {
    return buildErrorResult();
  }

  @Override
  protected Object handleSchemaGenerationFailure() {
    return buildErrorResult();
  }

  @Override
  protected Object handleAiServiceFailure() {
    return buildErrorResult();
  }

  @Override
  protected Object handleExecutionException(Exception e) {
    Ivy.log().error(String.format("DataMapping execution failed: %s", e.getMessage()));
    return buildErrorResult();
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

  /**
   * Determines if the result from AI is valid and successful by attempting to
   * parse it back to the target object.
   */
  private boolean isValidResult(Object result) {
    return result != null;
  }

  private Object buildErrorResult() {
    return null;
  }

  /**
   * Builds a {@link JsonObjectSchema} representing the fields of the class.
   * Enhanced version that applies field explanations and required field logic.
   *
   * @param clazz the class to process
   * @return the object schema with property mappings
   */
  private JsonObjectSchema buildObjectSchemaWithExplanations(Class<?> clazz) {
    JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
    List<String> requiredFields = new ArrayList<>();

    Field[] fields = clazz.getDeclaredFields();

    if (fields.length == 0) {
      return null;
    }

    for (Field field : fields) {
      // Skip static and synthetic fields
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
        continue;
      }

      String fieldName = field.getName();
      // Use the utility method from JsonSchemaAiFunction for basic type mapping
      JsonSchemaElement fieldSchema = createSchemaElementForTypeWithLimitedSupport(field.getType(), field);

      // Skip unsupported field
      if (fieldSchema == null) {
        continue;
      }

      // Add description from FieldExplanation if available
      if (CollectionUtils.isNotEmpty(fieldExplanations)) {
        for (FieldExplanation explanation : fieldExplanations) {
          if (explanation.getName().equals(fieldName)) {
            // Use the utility method from JsonSchemaAiFunction
            fieldSchema = addDescriptionToSchema(fieldSchema, explanation);

            // If the field is marked as mandatory, add it to the required fields list
            if (BooleanUtils.isTrue(explanation.isMandatory())) {
              requiredFields.add(fieldName);
            }
            break;
          }
        }
      }

      builder.addProperty(fieldName, fieldSchema);
    }

    // Set required fields
    if (!requiredFields.isEmpty()) {
      builder.required(requiredFields.toArray(new String[0]));
    }

    return builder.build();
  }

  /**
   * Maps a Java type to an appropriate {@link JsonSchemaElement}. Limited version
   * that doesn't support arrays, lists, and nested objects (DataMapping
   * requirement).
   *
   * @param type  the field type
   * @param field the field itself (unused here but could be extended)
   * @return a schema element representing the field's type, null for unsupported
   *         types
   */
  private JsonSchemaElement createSchemaElementForTypeWithLimitedSupport(Class<?> type, Field field) {
    // Handle primitive and wrapper types
    if (type == String.class || type == char.class || type == Character.class) {
      return JsonStringSchema.builder().build();
    }

    if (type == int.class || type == Integer.class || type == long.class || type == Long.class
        || type == BigInteger.class) {
      return JsonIntegerSchema.builder().build();
    }

    if (type == float.class || type == Float.class || type == double.class || type == Double.class
        || type == BigDecimal.class) {
      return JsonNumberSchema.builder().build();
    }

    if (type == boolean.class || type == Boolean.class) {
      return JsonBooleanSchema.builder().build();
    }

    // Handle enums
    if (type.isEnum()) {
      Object[] enumConstants = type.getEnumConstants();
      List<String> enumValues = new ArrayList<>();
      for (Object enumConstant : enumConstants) {
        enumValues.add(enumConstant.toString());
      }
      return JsonEnumSchema.builder().enumValues(enumValues).build();
    }

    // DataMapping doesn't support arrays, lists, and nested objects
    return null;
  }

  // Getters and setters for domain-specific fields
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

  public Boolean getAsList() {
    return asList;
  }

  public void setAsList(Boolean asList) {
    this.asList = asList;
  }

//Builder class for DataMapping
  public static class Builder extends AiFunction.Builder<Object, Object, DataMapper> {
    private Object targetObject;
    private List<FieldExplanation> fieldExplanations;
    private Boolean asList;
    private List<AiExample> examples = new ArrayList<>();

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
    public Builder addInputData(List<Object> data) {
      return (Builder) super.addInputData(data);
    }

    public Builder addFieldExplanations(List<FieldExplanation> fieldExplanations) {
      if (this.fieldExplanations == null) {
        this.fieldExplanations = new ArrayList<>();
      }
      this.fieldExplanations.addAll(fieldExplanations);
      return this;
    }

    public Builder addExamples(List<AiExample> examples) {
      if (CollectionUtils.isNotEmpty(examples)) {
        this.examples.addAll(examples);
      }
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

    public Builder asList(Boolean asList) {
      this.asList = asList;
      return this;
    }

    @Override
    public DataMapper build() {
      DataMapper dataMapper = new DataMapper();
      dataMapper.setConnector(connector);
      dataMapper.setQuery(query);
      dataMapper.setCustomInstructions(customInstructions);
      dataMapper.setTargetObject(targetObject);
      dataMapper.setFieldExplanations(fieldExplanations);
      dataMapper.setAsList(asList);
      return dataMapper;
    }
  }
}