package com.axonivy.utils.ai.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.ExecutionStatus;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;

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
  @SuppressWarnings("restriction")
  public String startExecution(Object input, String username, String agentName, List<CallSubStart> tools,
      List<Instruction> instructions, String goal, Integer maxIterations) {
    try {
      // Step 1: Create AgentExecution context
      AgentExecution execution = new AgentExecution(agentName, username, input, tools, instructions, goal,
          maxIterations);
      execution.setStatus(ExecutionStatus.PENDING);

      // Store execution for tracking
      executions.put(execution.getId(), execution);

      // Step 2: Start agent execution with context
      IvyAgent agent = new IvyAgent();
      ExecutionStatus status = agent.start(execution);
      execution.setStatus(ExecutionStatus.DONE);

      Ivy.log()
          .info(
              "Execution " + execution.getId() + " for agent " + agentName + " by user " + username + " is completed");
      return execution.getId();

    } catch (Exception e) {
      Ivy.log().error("Failed to start execution for agent " + agentName + ": " + e.getMessage(), e);
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
}
