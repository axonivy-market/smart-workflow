package com.axonivy.utils.ai.dto.ai.configuration;

import java.util.List;

import com.axonivy.utils.ai.dto.ai.Instruction;

public class ToolModel {
  private String processSignature;
  private List<Instruction> instructions;

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  public String getProcessSignature() {
    return processSignature;
  }

  public void setProcessSignature(String processSignature) {
    this.processSignature = processSignature;
  }
}
