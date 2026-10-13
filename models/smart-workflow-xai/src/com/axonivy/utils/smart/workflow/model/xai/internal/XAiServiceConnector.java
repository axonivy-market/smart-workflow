package com.axonivy.utils.smart.workflow.model.xai.internal;

import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.DEFAULT_TEMPERATURE;
import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveTemperature;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

public class XAiServiceConnector {

  private static final String DEFAULT_MODEL = "grok-4-1-fast";
  private static final String DEFAULT_BASE_URL = "https://api.x.ai/v1/";

  public static final List<String> SUPPORTED_MODELS = List.of(
      "grok-4-1-fast",
      "grok-4-1-mini",
      "grok-4-1-large",
      "grok-4-1-max",
      "grok-4-1-mini-code",
      "grok-4-1-large-code",
      "grok-4-1-max-code");

  public interface XAiConf {
    String PREFIX = "AI.Providers.xAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return initBuilder(resolveModelName(DEFAULT_MODEL), DEFAULT_TEMPERATURE);
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return initBuilder(resolveModelName(DEFAULT_MODEL), DEFAULT_TEMPERATURE)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  public static OpenAiChatModelBuilder buildOpenAiModel(ModelOptions options) {
    return initBuilder(resolveModelName(options.modelName()), options.temperature());
  }
          
  public static OpenAiChatModelBuilder buildJsonOpenAiModel(ModelOptions options) {
    return initBuilder(resolveModelName(options.modelName()), options.temperature())
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  private static OpenAiChatModelBuilder initBuilder(String modelName, Double temperature) {
    OpenAiChatModelBuilder builder = initBuilder();
    var request = ChatRequestParameters.builder()
        .modelName(modelName)
        .temperature(resolveTemperature(temperature));
    builder.defaultRequestParameters(request.build());
    return builder;
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true);
    var baseUrl = StringUtils.defaultIfBlank(Ivy.var().get(XAiConf.BASE_URL), DEFAULT_BASE_URL);
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
