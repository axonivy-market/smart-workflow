package com.axonivy.utils.smart.workflow.gemini.enums;

public enum GoogleAiGeminiChatModelName {
  // Gemini 2.5 Models
  GEMINI_2_5_PRO("gemini-2.5-pro"),
  GEMINI_2_5_FLASH("gemini-2.5-flash"),

  // Gemini 2.0 Models
  GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp"),
  GEMINI_2_0_FLASH("gemini-2.0-flash"),

  // Gemini 1.5 Models
  GEMINI_1_5_PRO("gemini-1.5-pro"),
  GEMINI_1_5_FLASH("gemini-1.5-flash");

  private final String name;

  GoogleAiGeminiChatModelName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
