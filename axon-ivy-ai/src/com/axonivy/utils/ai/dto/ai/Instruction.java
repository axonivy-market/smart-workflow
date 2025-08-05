package com.axonivy.utils.ai.dto.ai;

import com.axonivy.utils.ai.enums.InstructionType;

public class Instruction {
  private InstructionType type;
  private String content;
  private String toolName;

  public static Instruction createPlanningInstruction(String content) {
    Instruction planningInstruction = new Instruction();
    planningInstruction.setType(InstructionType.PLANNING);
    planningInstruction.setContent(content);
    return planningInstruction;
  }

  public static Instruction createExecutionInstruction(String toolName, String content) {
    Instruction executionInstruction = new Instruction();
    executionInstruction.setType(InstructionType.EXECUTION);
    executionInstruction.setToolName(toolName);
    executionInstruction.setContent(content);
    return executionInstruction;
  }

  public InstructionType getType() {
    return type;
  }

  public void setType(InstructionType type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getToolName() {
    return toolName;
  }

  public void setToolName(String toolName) {
    this.toolName = toolName;
  }
}
