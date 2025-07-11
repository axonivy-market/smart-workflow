package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.StringProcessingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.input.PromptTemplate;

public class DataMapping extends AiFunction {

  private static final String INSTRUCTION_TEMPLATE = """
      JSON object:
          {{object}}

          + Analyze the query, find corresponding attributes to fill
          + don't change the text from the query, don't generate your own text
          + The result MUST be the fulfilled JSON object, not the updated parts
          {{asListInstruction}}
      """;

  private static final String AS_LIST_INSTRUCTION = """
          + The result MUST be a list of JSON objects above
      """;

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

  // Convert the targetObject to JSON string using Jackson
  public String convertObjectToJson() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(targetObject);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

  public String getFormattedFieldExplanations() {
    String result = StringUtils.EMPTY;

    if (fieldExplanations == null || fieldExplanations.isEmpty()) {
      return result;
    }

    // Loop the field explanations and format them
    for (FieldExplanation explanation : fieldExplanations) {
      String formattedExplanation = String.format(EXPLANATION_FORMAT, explanation.getName(),
          explanation.getExplanation());
      result += formattedExplanation;
    }

    // Return the formatted explanations template
    Map<String, Object> params = new HashMap<>();
    params.put("explanations", result.strip());
    return PromptTemplate.from(EXPLANATION_TEMPLATE).apply(params).text();
  }

  @Override
  protected void buildInstructions() {

    // Convert object to JSON
    String convertedJson = convertObjectToJson();

    // Build the function instructions
    Map<String, Object> params = new HashMap<>();
    params.put("object", convertedJson);
    params.put("asListInstruction", BooleanUtils.isTrue(asList) ? AS_LIST_INSTRUCTION : StringUtils.EMPTY);
    setFunctionInstructions(PromptTemplate.from(INSTRUCTION_TEMPLATE).apply(params).text());

    // Build the custom instructions
    if (getCustomInstructions() == null) {
      setCustomInstructions(new ArrayList<>());
    }
    getCustomInstructions().addAll(Arrays.asList(getFormattedFieldExplanations()));

  }

  @Override
  protected String standardizeResult(String result) {
    // Extract the JSON from the wrapper (<< >>)
    String standardResult = super.standardizeResult(result);

    // Use StringProcessingUtils to check and fix JSON if needed
    return StringProcessingUtils.standardizeResult(standardResult);
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
  }

  // Builder class for DataMapper
  public static class Builder extends AiFunction.Builder {
    private Object targetObject;
    private Boolean asList;
    private List<FieldExplanation> fieldExplanations;

    public Builder withTargetJson(String json) {
      try {
        this.targetObject = BusinessEntityConverter.jsonValueToEntity(json, Object.class);
      } catch (Exception e) {
        this.targetObject = null;
      }
      return this;
    }

    public Builder withTargetObject(Object targetObject) {
      this.targetObject = targetObject;
      return this;
    }

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
      if (StringUtils.isNotBlank(query)) {
        this.examples.addAll(examples);
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

      return dataMapper;
    }
  }
}