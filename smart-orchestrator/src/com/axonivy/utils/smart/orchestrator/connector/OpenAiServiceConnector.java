package com.axonivy.utils.smart.orchestrator.connector;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.client.SmartHttpClientBuilderFactory;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;

  private static final String DEFAULT_MODEL = OpenAiChatModelName.GPT_4_1_MINI.toString();

  public interface OpenAiConf {
    String PREFIX = "Ai.OpenAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String MODEL = PREFIX + "Model";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return buildOpenAiModel(DEFAULT_MODEL);
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return buildJsonOpenAiModel(DEFAULT_MODEL);
  }

  public static OpenAiChatModelBuilder buildOpenAiModel(String modelName) {
    return initBuilder(validateModelName(modelName));
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel(String modelName) {
    return initBuilder(validateModelName(modelName))
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  private static OpenAiChatModelBuilder initBuilder(String modelName) {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true)
        .modelName(modelName);
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

    // Only set temperature if not using the "o" series
    if (!modelName.startsWith("o")) {
      builder.temperature(Double.valueOf(DEFAULT_TEMPERATURE));
    }

    return builder;
  }

  private static String validateModelName(String modelName) {
    String candidate = StringUtils.isBlank(modelName)
        ? Ivy.var().get(OpenAiConf.MODEL)
        : modelName;

    for (var model : OpenAiChatModelName.values()) {
      if (model.toString().equals(modelName)) {
        return candidate;
      }
    }
    return DEFAULT_MODEL;
  }
}
