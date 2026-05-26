package com.axonivy.utils.smart.workflow.model.xai.internal;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

public class XAiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;
  private static final String DEFAULT_MODEL = "grok-4-1-fast";
  private static final String DEFAULT_BASE_URL = "https://api.x.ai/v1/";

  public static final List<String> SUPPORTED_MODELS = List.of(
      "grok-4-1-fast",
      "grok-4-1-mini",
      "grok-4-1-large",
      "grok-4-1-max",
      "grok-4-1-mini-code",
      "grok-4-1-large-code",
      "grok-4-1-max-code"
  );

  public interface XAiConf {
    String PREFIX = "AI.Providers.xAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return buildOpenAiModel(DEFAULT_MODEL);
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return buildJsonOpenAiModel(DEFAULT_MODEL);
  }

  public static OpenAiChatModelBuilder buildOpenAiModel(String modelName) {
    return initBuilder(resolveModelName(modelName));
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel(String modelName) {
    return initBuilder(resolveModelName(modelName))
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  private static OpenAiChatModelBuilder initBuilder(String modelName) {
    OpenAiChatModelBuilder builder = initBuilder();
    var request = ChatRequestParameters.builder()
      .modelName(modelName)
      .temperature(Double.valueOf(DEFAULT_TEMPERATURE));
    builder.defaultRequestParameters(request.build()); 
    return builder;
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true);
    var baseUrl = StringUtils.defaultIfBlank(Ivy.var().get(XAiConf.BASE_URL) , DEFAULT_BASE_URL);
    builder.baseUrl(baseUrl);
    String key = Ivy.var().get(XAiConf.API_KEY);
    if (!key.isBlank()) {
      builder.apiKey(key);
    } else {
      builder.customHeaders(Map.of("X-Requested-By", "ivy")); // TODO as pure test variable
    }
    return builder;
  }

  private static String resolveModelName(String modelName) {
    String selected = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(XAiConf.DEFAULT_MODEL), DEFAULT_MODEL));
    validateModel(selected);
    return selected;
  }

  private static void validateModel(String modelName) {
    if (!SUPPORTED_MODELS.contains(modelName)) {
      Ivy.log().warn("Unknown xAI model: '" + modelName + "'. Compatibility not guaranteed.");
    }
  }
}
