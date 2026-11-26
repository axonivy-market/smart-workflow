package com.axonivy.utils.smart.workflow.model.anthropic;

import java.util.List;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector;
import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils;

import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

public class AnthropicModelProvider implements ChatModelProvider {

  public static final String NAME = "Anthropic";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    // waits for https://github.com/langchain4j/langchain4j/issues/7284
    ModelOptionUtils.warnReasoningEffortUnsupported(NAME, options.reasoningEffort());
    var builder = AnthropicServiceConnector.buildAnthropicModel(options);
    if (options.structuredOutput()) {
      builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
    }
    builder.listeners(options.listeners());
    return builder.build();
  }

  @Override
  public List<String> models() {
    return Stream.of(AnthropicChatModelName.values())
        .map(AnthropicChatModelName::toString)
        .toList();
  }

  @Override
  public List<String> secretsVars() {
    return List.of(AnthropicConf.API_KEY);
  }

}
