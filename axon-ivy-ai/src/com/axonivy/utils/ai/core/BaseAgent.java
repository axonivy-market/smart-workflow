package com.axonivy.utils.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.ExecutionStatus;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract base class for all AI agents. Contains common fields and functionality
 * that all agent types share.
 */
public abstract class BaseAgent {

  protected static final int DEFAULT_MAX_ITERATIONS = 20; // Default iteration limit
  protected static final String ONE_LINE = System.lineSeparator();
  protected static final String TWO_LINES = System.lineSeparator() + System.lineSeparator();

  // Unique identifier for the agent
  protected String id;

  // Human-readable name for the agent
  protected String name;

  // Description or intended usage of the agent
  protected String usage;

  // Enhanced Configuration Fields
  protected int maxIterations = DEFAULT_MAX_ITERATIONS; // Configurable iteration limit
  protected AbstractAiServiceConnector DEFAULT_CONNECTOR = OpenAiServiceConnector.getTinyBrain();

  // Dual AI Model Architecture
  protected AbstractAiServiceConnector planningModel; // For plan generation
  protected AbstractAiServiceConnector executionModel; // For step execution & reasoning

  // Instruction System
  protected List<Instruction> instructions;                 // Planning and execution instructions

  protected List<IvyTool> tools;

  // List of results collected during agent execution (not serialized to JSON)
  @JsonIgnore
  protected List<AiVariable> results;

  public BaseAgent() {
    id = IdGenerationUtils.generateRandomId();
  }

  protected List<String> getPlanningInstructions() {
    if (instructions == null || instructions.isEmpty()) {
      return new ArrayList<>();
    }

    return instructions.stream().filter(instruction -> instruction.getType() == InstructionType.PLANNING)
        .filter(instruction -> StringUtils.isNotBlank(instruction.getContent().strip())).map(Instruction::getContent)
        .collect(Collectors.toList());
  }

  /**
   * Builds a description of available worker agents
   */
  protected String buildAvailableToolsDescription() {
    StringBuilder toolsStr = new StringBuilder();
    for (var tool : tools) {
      toolsStr.append("- ID: ").append(tool.getId()).append(", Name: ").append(tool.getName()).append(", Usage: ")
          .append(tool.getUsage()).append(System.lineSeparator());
    }
    return toolsStr.toString();
  }

  /**
   * Template method - starts the agent with a user query.
   *
   * @param execution The agent execution.
   */
  public abstract ExecutionStatus start(AgentExecution execution);

  /**
   * Template method - executes the agent's plan.
   */
  public abstract void execute(AgentExecution execution);

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public List<AiVariable> getResults() {
    return results;
  }

  public void setResults(List<AiVariable> results) {
    this.results = results;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  public AbstractAiServiceConnector getPlanningModel() {
    return planningModel;
  }

  public void setPlanningModel(AbstractAiServiceConnector planningModel) {
    this.planningModel = planningModel;
  }

  public AbstractAiServiceConnector getExecutionModel() {
    return executionModel;
  }

  public void setExecutionModel(AbstractAiServiceConnector executionModel) {
    this.executionModel = executionModel;
  }
}
