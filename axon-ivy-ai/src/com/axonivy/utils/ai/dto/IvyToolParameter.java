package com.axonivy.utils.ai.dto;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class IvyToolParameter implements Serializable {

  private static final long serialVersionUID = 630365402103731067L;

  private String name;
  private String value;
  private String description;
  private Boolean isMandatory;

  public IvyToolParameter() {
  }

  public IvyToolParameter(String name, String value, String description) {
    this.name = name;
    this.value = value;
    this.description = description;
  }

  public IvyToolParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = StringUtils.EMPTY;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
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
}