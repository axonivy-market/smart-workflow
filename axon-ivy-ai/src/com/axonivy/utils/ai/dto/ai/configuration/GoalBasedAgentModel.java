package com.axonivy.utils.ai.dto.ai.configuration;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.ai.dto.AbstractConfiguration;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.InstructionType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalBasedAgentModel extends AbstractConfiguration {

  private String name;
  private String usage;
  private String model;
  private List<String> tools;

  private String goal;
  private int maxIterations;

  // Dual AI Model Architecture
  private String planningModel;
  private String planningModelKey;
  private String executionModel;
  private String executionModelKey;

  // Instruction System
  private List<Instruction> instructions; // Planning and execution instructions

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

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public String getPlanningModel() {
    return planningModel;
  }

  public void setPlanningModel(String planningModel) {
    this.planningModel = planningModel;
  }

  public String getPlanningModelKey() {
    return planningModelKey;
  }

  public void setPlanningModelKey(String planningModelKey) {
    this.planningModelKey = planningModelKey;
  }

  public String getExecutionModel() {
    return executionModel;
  }

  public void setExecutionModel(String executionModel) {
    this.executionModel = executionModel;
  }

  public String getExecutionModelKey() {
    return executionModelKey;
  }

  public void setExecutionModelKey(String executionModelKey) {
    this.executionModelKey = executionModelKey;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  /**
   * Helper method to add a planning instruction
   */
  public void addPlanningInstruction(String content) {
    if (instructions == null) {
      instructions = new ArrayList<>();
    }
    Instruction instruction = new Instruction();
    instruction.setType(InstructionType.PLANNING);
    instruction.setContent(content);
    instructions.add(instruction);
  }

  /**
   * Helper method to add an execution instruction
   */
  public void addExecutionInstruction(String content) {
    if (instructions == null) {
      instructions = new ArrayList<>();
    }
    Instruction instruction = new Instruction();
    instruction.setType(InstructionType.EXECUTION);
    instruction.setContent(content);
    instructions.add(instruction);
  }

  /**
   * Helper method to add an execution instruction for a specific tool
   */
  public void addExecutionInstruction(String content, String toolId) {
    if (instructions == null) {
      instructions = new ArrayList<>();
    }
    Instruction instruction = new Instruction();
    instruction.setType(InstructionType.EXECUTION);
    instruction.setContent(content);
    instruction.setToolId(toolId);
    instructions.add(instruction);
  }
}