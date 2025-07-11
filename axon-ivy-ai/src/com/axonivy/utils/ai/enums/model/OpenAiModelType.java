package com.axonivy.utils.ai.enums.model;

public enum OpenAiModelType {
  GPT_4O("gpt-4o"), GPT_4O_MINI("gpt-4o-mini");

  private String name;

  private OpenAiModelType(String name) {
    this.setName(name);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static OpenAiModelType getDefault() {
    return GPT_4O_MINI;
  }

  public static OpenAiModelType findType(String name) {
    for (OpenAiModelType t : OpenAiModelType.values()) {
      if (t.getName().contentEquals(name)) {
        return t;
      }
    }
    return getDefault();
  }
}
