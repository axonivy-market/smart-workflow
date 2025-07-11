package com.axonivy.utils.ai.service;

import com.axonivy.utils.ai.dto.ai.configuration.GoalBasedAgentModel;

/**
 * Service class for managing goal-based agent configurations. Handles CRUD
 * operations for goal-based agents including file persistence.
 */
public class GoalBasedAgentService extends JsonConfigurationService<GoalBasedAgentModel> {

  private static GoalBasedAgentService instance;

  public static GoalBasedAgentService getInstance() {
    if (instance == null) {
      instance = new GoalBasedAgentService();
    }
    return instance;
  }

  @Override
  public Class<GoalBasedAgentModel> getType() {
    return GoalBasedAgentModel.class;
  }

  @Override
  public String getConfigKey() {
    return "AI.Agents";
  }
}