package com.axonivy.utils.smart.workflow.azureopenai;

public class AzureAiDeployment {
  private String name;
  private String model;
  private String apiKey;

  public AzureAiDeployment(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
