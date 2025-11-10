package com.axonivy.utils.smart.workflow.model.spi;

import java.util.List;

import dev.langchain4j.model.chat.ChatModel;

public interface ChatModelProvider {

  String name();
  ChatModel setup(ModelOptions options);
  List<String> models();

  public static record ModelOptions(
      String modelName,
      boolean structuredOutput) {

    public ModelOptions() {
      this(null, false);
    }

    public static ModelOptions options() {
      return new ModelOptions();
    }

    public ModelOptions structuredOutput(boolean structured) {
      return new ModelOptions(modelName, structured);
    }

    public ModelOptions modelName(String name) {
      return new ModelOptions(name, structuredOutput);
    }
  }

}
