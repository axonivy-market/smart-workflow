package com.axonivy.utils.ai.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.axonivy.utils.ai.dto.ai.configuration.AgentModel;
import com.axonivy.utils.ai.enums.ExecutionStatus;
import com.axonivy.utils.ai.service.AgentService;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Orchestrates agent executions and manages execution contexts. Provides the
 * main entry point for starting and managing AI agent executions.
 */
public class AgentExecutor {
  // In-memory storage for execution contexts
  private final Map<String, AgentExecution> executions = new ConcurrentHashMap<>();

  /**
   * Main entry point: Starts a new agent execution
   * 
   * Step 1: Load agent model from configuration
   * Step 2: Creates AgentExecution context
   * Step 3: Loads and configures agent
   * Step 4: Starts agent execution with context
   * 
   * @param agentId  The ID of the agent to execute
   * @param query    The user query/input
   * @param username The username initiating the execution
   * @return The UUID of the created execution
   */
  public String startExecution(String agentId, String query, String username) {
    try {
      // Step 1: Initialize agent from configuration
      AgentModel agentModel = AgentService.getInstance().findById(agentId);
      if (agentModel == null) {
        return null;
      }
      IvyAgent agent = new IvyAgent();
      agent.loadFromModel(agentModel);

      // Step 2: Create AgentExecution context
      AgentExecution execution = createExecutionContext(agentId, query, username);

      // Store execution for tracking
      executions.put(execution.getId(), execution);

      // Step 3: Start agent execution with context
      ExecutionStatus status = agent.start(execution);
      execution.setStatus(ExecutionStatus.DONE);

      Ivy.log()
          .info("Execution " + execution.getId() + " for agent " + agentId + " by user " + username + " is completed");
      return execution.getId();

    } catch (Exception e) {
      Ivy.log().error("Failed to start execution for agent " + agentId + ": " + e.getMessage(), e);
      throw new RuntimeException("Failed to start agent execution", e);
    }
  }

  /**
   * Gets the execution context by ID
   */
  public AgentExecution getExecution(String executionId) {
    return executions.get(executionId);
  }

  /**
   * Gets the status of an execution
   */
  public ExecutionStatus getExecutionStatus(String executionId) {
    AgentExecution execution = executions.get(executionId);
    return execution != null ? execution.getStatus() : null;
  }

  /**
   * Creates a new execution context with initial state
   */
  private AgentExecution createExecutionContext(String agentId, String query, String username) {
    AgentExecution execution = new AgentExecution(agentId, query, username);
    execution.setStatus(ExecutionStatus.PENDING);

    Ivy.log().info("Created execution context " + execution.getId() + " for agent " + agentId);
    return execution;
  }
}
