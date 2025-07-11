package com.axonivy.utils.ai.dto.ivy;

import java.io.Serializable;
import java.util.Map;

public class TaskInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private String description;
  private String responsible;
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