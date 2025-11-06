package com.axonivy.utils.smart.workflow.model.openai;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiModelProvider implements ChatModelProvider {

  public static final String NAME = "OpenAI";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(options.modelName());
    if (options.structuredOutput()) {
      builder.responseFormat("json_schema");
    }
    return builder.build();
  }

  @Override
  public List<String> models() {
    return Stream.of(OpenAiChatModelName.values())
        .map(OpenAiChatModelName::name)
        .toList();
  }

}
