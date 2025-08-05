package com.axonivy.utils.ai.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class IvyToolParameter implements Serializable {

  private static final long serialVersionUID = 630365402103731067L;

  @SuppressWarnings("restriction")
  private VariableDesc definition;

  @JsonIgnore
  private Object value;

  public IvyToolParameter() {
  }

  @SuppressWarnings("restriction")
  public IvyToolParameter(VariableDesc definition, Object value) {
    this.definition = definition;
    this.value = value;
  }

  @SuppressWarnings("restriction")
  public IvyToolParameter(VariableDesc definition) {
    this.definition = definition;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @SuppressWarnings("restriction")
  public VariableDesc getDefinition() {
    return definition;
  }

  @SuppressWarnings("restriction")
  public void setDefinition(VariableDesc definition) {
    this.definition = definition;
  }
}