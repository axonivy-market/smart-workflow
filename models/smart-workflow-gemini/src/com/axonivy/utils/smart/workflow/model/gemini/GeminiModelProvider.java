package com.axonivy.utils.smart.workflow.model.gemini;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.environment.Ivy;
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
      // Gemini currently does NOT support structured JSON responses.
      // Reference:
      // https://discuss.ai.google.dev/t/function-calling-with-a-response-mime-type-application-json-is-unsupported/105093
      Ivy.log().error("Structured output is unsupported.");
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
