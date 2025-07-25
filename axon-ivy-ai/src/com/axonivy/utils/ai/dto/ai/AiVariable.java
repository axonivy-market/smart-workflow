package com.axonivy.utils.ai.dto.ai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.dto.IvyToolParameter;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Represents a named variable that can be passed into or produced by an AI step
 * or agent. This includes metadata like a description and whether the variable
 * is mandatory.
 */
@JsonInclude(value = Include.NON_EMPTY)
public class AiVariable {

  private static final String PRETTY_TEMPLATE = """
      Id: {{id}}
      State: {{state}}
      Parameter:
      {{param}}
      """;

  public AiVariable() {
  }

  public AiVariable(String name, String content) {
    this.id = IdGenerationUtils.generateRandomId();
    this.parameter = new IvyToolParameter(name, content, "");
    this.state = AiVariableState.SUCCESS;
  }

  // Unique identifier for the variable (auto-generated if not explicitly set)
  private String id;

  // IvyToolParameter containing name, value, description, etc.
  private IvyToolParameter parameter;

  private AiVariableState state;

  /**
   * Default constructor that auto-generates a unique ID without hyphens.
   */
  @JsonIgnore
  public void init() {
    this.id = IdGenerationUtils.generateRandomId();
    this.parameter = new IvyToolParameter();
    this.state = AiVariableState.EMPTY;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public IvyToolParameter getParameter() {
    return parameter;
  }

  public void setParameter(IvyToolParameter parameter) {
    this.parameter = parameter;
  }

  public AiVariableState getState() {
    return state;
  }

  public void setState(AiVariableState state) {
    this.state = state;
  }

  /**
   * Creates an example variable instance for use in Data Mapping service
   *
   * @return an example {@link AiVariable} with preset name, content, and
   *         description.
   */
  public static AiVariable getMappingExample() {
    AiVariable example = new AiVariable();
    example.setId(IdGenerationUtils.generateRandomId());
    example.setParameter(new IvyToolParameter("Example variable", "Just an example", "Example description"));
    example.setState(AiVariableState.SUCCESS);
    return example;
  }

  public static List<AiVariable> getMappingExampleList() {
    return Arrays.asList(getMappingExample());
  }

  public static List<String> getExampleIdList() {
    String id1 = IdGenerationUtils.generateRandomId();
    String id2 = IdGenerationUtils.generateRandomId();
    String id3 = IdGenerationUtils.generateRandomId();
    return Arrays.asList(id1, id2, id3);
  }

  @JsonIgnore
  public String getSafeValue() {
    Object value = Optional.ofNullable(this).map(AiVariable::getParameter).map(IvyToolParameter::getValue)
        .orElse(StringUtils.EMPTY);
    return value instanceof String ? (String) value : BusinessEntityConverter.entityToJsonValue(value);
  }

  public String toPrettyString() {
    Map<String, Object> params = new HashMap<>();
    params.put("id", id);
    params.put("state", state);
    params.put("param", BusinessEntityConverter.entityToJsonValue(parameter));
    return PromptTemplate.from(PRETTY_TEMPLATE).apply(params).text();
  }
}