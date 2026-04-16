package com.axonivy.utils.smart.workflow.model.spi;

import java.util.List;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

public interface ChatModelProvider {

  String name();
  ChatModel setup(ModelOptions options);
  List<String> models();
  List<String> secretsVars();

  public static record ModelOptions(
      String modelName,
      boolean structuredOutput,
      List<ChatModelListener> listeners) {

    public ModelOptions() {
      this(null, false, List.of());
    }

    public static ModelOptions options() {
      return new ModelOptions();
    }

    public ModelOptions structuredOutput(boolean structured) {
      return new ModelOptions(modelName, structured, listeners);
    }

    public ModelOptions modelName(String name) {
      return new ModelOptions(name, structuredOutput, listeners);
    }

    public ModelOptions listeners(List<ChatModelListener> chatListeners) {
      return new ModelOptions(modelName, structuredOutput, chatListeners);
    }
  }

}
