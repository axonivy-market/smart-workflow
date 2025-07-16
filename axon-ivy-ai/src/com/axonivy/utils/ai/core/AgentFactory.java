package com.axonivy.utils.ai.core;

import com.axonivy.utils.ai.dto.ai.configuration.GoalBasedAgentModel;
import com.axonivy.utils.ai.enums.AgentType;

/**
 * Factory class for creating different types of AI agents.
 * Uses the Factory pattern to instantiate the appropriate agent type
 * based on configuration.
 */
public class AgentFactory {

  /**
   * Creates an agent instance based on the specified AgentType.
   * 
   * @param type The type of agent to create
   * @return A new instance of the specified agent type
   * @throws IllegalArgumentException if the agent type is unknown
   */
  public static BaseAgent createAgent(AgentType type) {
    switch (type) {
      case STEP_BY_STEP:
        return new IvyAgent();
      case TODO_LIST:
        return new TodoAgent();
      default:
        throw new IllegalArgumentException("Unknown agent type: " + type);
    }
  }

  /**
   * Creates an agent instance based on the agent type string.
   * 
   * @param agentTypeString The string representation of the agent type
   * @return A new instance of the specified agent type
   * @throws IllegalArgumentException if the agent type string is unknown
   */
  public static BaseAgent createAgent(String agentTypeString) {
    try {
      AgentType type = AgentType.valueOf(agentTypeString.toUpperCase());
      return createAgent(type);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown agent type string: " + agentTypeString, e);
    }
  }

  /**
   * Creates and configures an agent from a GoalBasedAgentModel.
   * This method creates the appropriate agent type and loads the configuration.
   * 
   * @param model The configuration model for the agent
   * @return A configured agent instance ready for use
   * @throws IllegalArgumentException if the agent type in the model is unknown
   */
  public static BaseAgent createAgent(GoalBasedAgentModel model) {
    // Determine agent type from model (defaulting to STEP_BY_STEP for backward compatibility)
    AgentType agentType = AgentType.STEP_BY_STEP; // Default
    
    if (model.getAgentType() != null) {
      agentType = model.getAgentType();
    }

    // Create the agent
    BaseAgent agent = createAgent(agentType);
    
    // Load configuration from model
    agent.loadFromModel(model);
    
    return agent;
  }

  /**
   * Creates and configures an agent from a GoalBasedAgentModel with explicit agent type override.
   * 
   * @param model The configuration model for the agent
   * @param agentType The agent type to create (overrides model's agent type)
   * @return A configured agent instance ready for use
   */
  public static BaseAgent createAgent(GoalBasedAgentModel model, AgentType agentType) {
    BaseAgent agent = createAgent(agentType);
    agent.loadFromModel(model);
    return agent;
  }

  /**
   * Gets the default agent type used when no type is specified.
   * 
   * @return The default agent type (STEP_BY_STEP for backward compatibility)
   */
  public static AgentType getDefaultAgentType() {
    return AgentType.STEP_BY_STEP;
  }

  /**
   * Checks if the given agent type is supported by the factory.
   * 
   * @param type The agent type to check
   * @return true if the agent type is supported, false otherwise
   */
  public static boolean isAgentTypeSupported(AgentType type) {
    try {
      createAgent(type);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Gets an array of all supported agent types.
   * 
   * @return Array of all supported AgentType values
   */
  public static AgentType[] getSupportedAgentTypes() {
    return AgentType.values();
  }
}