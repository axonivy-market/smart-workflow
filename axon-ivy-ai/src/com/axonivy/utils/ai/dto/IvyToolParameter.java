package com.axonivy.utils.ai.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class IvyToolParameter implements Serializable {

  private static final long serialVersionUID = 630365402103731067L;

  private String name;
  private String className;
  private String description;
  private Boolean isMandatory;

  @JsonIgnore
  private Object value;

  public IvyToolParameter() {
  }

  public IvyToolParameter(String name, Object value, String description) {
    this.name = name;
    this.value = value;
    this.description = description;
  }

  public IvyToolParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getIsMandatory() {
    return isMandatory;
  }

  public void setIsMandatory(Boolean isMandatory) {
    this.isMandatory = isMandatory;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }
}