package com.axonivy.utils.smart.workflow.model.azureopenai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.entity.AzureAiDeployment;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.utils.VariableUtils;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;

public class AzureOpenAiModelProvider implements ChatModelProvider {
  public static String NAME = "AzureOpenAI";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = AzureOpenAiServiceConnector.buildOpenAiModel(options.modelName());
    if (builder == null) {
      return null;
    }

    if (options.structuredOutput()) {
      builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA).strictJsonSchema(true)
          .responseFormat(ResponseFormat.JSON);
    }
    return builder.build();
  }

  @Override
  public List<String> models() {
    return Optional.ofNullable(VariableUtils.getDeployments()).orElse(new ArrayList<>()).stream()
        .map(AzureAiDeployment::getName).collect(Collectors.toList());
  }
}
