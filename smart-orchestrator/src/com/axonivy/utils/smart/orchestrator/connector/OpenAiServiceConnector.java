package com.axonivy.utils.smart.orchestrator.connector;

import java.util.Map;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;

  public interface OpenAiConf {
    String PREFIX = "Ai.OpenAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String MODEL = PREFIX + "Model";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return initBuilder();
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return initBuilder()
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .logRequests(true)
        .logResponses(true)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .temperature(Double.valueOf(DEFAULT_TEMPERATURE));
    var baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    String key = Ivy.var().get(OpenAiConf.API_KEY);
    if (!key.isBlank()) {
      builder.apiKey(key);
    } else {
      builder.customHeaders(Map.of("X-Requested-By", "ivy")); // TODO as pure test variable
    }
    var modelName = Ivy.var().get(OpenAiConf.MODEL);
    if (!modelName.isEmpty()) {
      builder.modelName(modelName);
    }
    return builder;
  }
}
