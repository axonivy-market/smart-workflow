package com.axonivy.utils.ai.service;

import com.axonivy.utils.ai.dto.ai.configuration.AgentModel;

public class AgentService extends JsonConfigurationService<AgentModel> {

  private static AgentService instance;

  public static AgentService getInstance() {
    if (instance == null) {
      instance = new AgentService();
    }
    return instance;
  }

  @Override
  public Class<AgentModel> getType() {
    return AgentModel.class;
  }

  @Override
  public String getConfigKey() {
    return "AI.Agents";
  }
}