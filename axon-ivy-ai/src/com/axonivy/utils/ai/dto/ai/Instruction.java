package com.axonivy.utils.ai.dto.ai;

import com.axonivy.utils.ai.enums.InstructionType;

public class Instruction {
  private InstructionType type;
  private String content;
  private String toolId;

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

  public String getToolId() {
    return toolId;
  }

  public void setToolId(String toolId) {
    this.toolId = toolId;
  }
}
