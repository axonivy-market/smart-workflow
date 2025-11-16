package com.axonivy.utils.smart.workflow.model.gemini;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.gemini.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.gemini.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

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
      builder.supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA));
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
