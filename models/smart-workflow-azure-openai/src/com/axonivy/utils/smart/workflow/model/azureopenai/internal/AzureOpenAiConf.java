package com.axonivy.utils.smart.workflow.model.azureopenai.internal;

public interface AzureOpenAiConf {
  String PREFIX = "AI.Providers.AzureOpenAI.";
  String ENDPOINT = PREFIX + "Endpoint";
  String DEPLOYMENTS = PREFIX + "Deployments";
  String DEFAULT_DEPLOYMENT = PREFIX + "DefaultDeployment";

  String MODEL_FIELD = "Model";
  String API_KEY_FIELD = "APIKey";
}
