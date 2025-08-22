package com.axonivy.utils.smart.orchestrator.demo.dto;

import java.io.Serializable;
import java.util.Map;

import dev.langchain4j.model.output.structured.Description;

public class TaskInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("The display name of the Axon Ivy task. This is a human-readable title that summarizes the task purpose.")
  private String name;

  @Description("A detailed explanation of what the task is about, including relevant context or objectives.")
  private String description;

  @Description("The username or identifier of the person currently responsible for performing or managing the task.")
  private String responsible;

  @Description("Custom key-value pairs containing additional task metadata. The key is the field name, and the value is its content. Represented as Map<String, String> in Java.")
  private Map<String, String> customFields;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getResponsible() {
    return responsible;
  }

  public void setResponsible(String responsible) {
    this.responsible = responsible;
  }

  public Map<String, String> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, String> customFields) {
    this.customFields = customFields;
  }
}