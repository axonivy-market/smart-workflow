package com.axonivy.utils.ai.dto.ai;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.enums.AiVariableState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a named variable that can be passed into or produced by an AI step
 * or agent. This includes metadata like a description and whether the variable
 * is mandatory.
 */
@JsonInclude(value = Include.NON_EMPTY)
public class AiVariable {

  public AiVariable() {
  }

  public AiVariable(String name, String content) {
    this.id = UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
    this.name = name;
    this.content = content;
    this.state = AiVariableState.SUCCESS;
  }

  // Unique identifier for the variable (auto-generated if not explicitly set)
  private String id;

  // Content or value of the variable
  private String content;

  // Human-readable description of the variable's purpose
  private String description;

  // Logical name of the variable (used to reference it in steps/tools)
  private String name;

  private AiVariableState state;

  /**
   * Default constructor that auto-generates a unique ID without hyphens.
   */
  @JsonIgnore
  public void init() {
    this.id = UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
    this.state = AiVariableState.EMPTY;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
    example.setId(UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY));
    example.setName("Example variable");
    example.setContent("Just an example");
    example.setDescription("Example description");
    example.setState(AiVariableState.SUCCESS);
    return example;
  }

  public static List<AiVariable> getMappingExampleList() {
    return Arrays.asList(getMappingExample());
  }
}