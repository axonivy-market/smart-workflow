package com.axonivy.utils.smart.workflow.model.xai;

import java.util.List;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.xai.internal.XAiServiceConnector;
import static com.axonivy.utils.smart.workflow.model.xai.internal.XAiServiceConnector.SUPPORTED_MODELS;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

public class XAiModelProvider implements ChatModelProvider {

  public static final String NAME = "xAI";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options, List<ChatModelListener> listeners) {
    var builder = XAiServiceConnector.buildOpenAiModel(options.modelName());
    if (options.structuredOutput()) {
      builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
      builder.strictJsonSchema(true);
      builder.responseFormat("json_schema");
    }
    builder.listeners(listeners);
    return builder.build();
  }

  @Override
  public List<String> models() {
    return SUPPORTED_MODELS;
  }

}
