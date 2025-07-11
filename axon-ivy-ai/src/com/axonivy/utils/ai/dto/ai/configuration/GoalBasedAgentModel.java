package com.axonivy.utils.ai.dto.ai.configuration;

import java.util.List;

import com.axonivy.utils.ai.dto.AbstractConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalBasedAgentModel extends AbstractConfiguration {

  private String name;
  private String usage;
  private String model;
  private List<String> tools;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsage() {
    return usage;
  }

  public void setUsage(String usage) {
    this.usage = usage;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public List<String> getTools() {
    return tools;
  }

  public void setTools(List<String> tools) {
    this.tools = tools;
  }
}