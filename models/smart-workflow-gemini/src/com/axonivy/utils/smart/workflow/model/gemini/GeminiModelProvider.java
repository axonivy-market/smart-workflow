package com.axonivy.utils.smart.workflow.model.gemini;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;

public class GeminiModelProvider implements ChatModelProvider {

  public static String NAME = "Gemini";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = GeminiServiceConnector.buildGeminiModel(options.modelName());
    if (options.structuredOutput()) {
      builder.defaultRequestParameters(ChatRequestParameters.builder().responseFormat(ResponseFormat.JSON).build());
    }
    return builder.build();
  }

  @Override
  public List<String> models() {
    return Stream.of(GoogleAiGeminiChatModelName.values())
        .map(GoogleAiGeminiChatModelName::toString)
        .toList();
  }

}
