package com.axonivy.utils.smart.workflow.model.openai.internal;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;

  // Temporary model name and temperature for GPT-5. Will remove once LangChain4j fully support GPT-5
  private static final String GPT_5 = "gpt-5";
  private static final int DEFAULT_TEMPERATURE_GPT_5 = 1;

  private static final String DEFAULT_MODEL = OpenAiChatModelName.GPT_4_1_MINI.toString();

  public interface OpenAiConf {
    String PREFIX = "Ai.Providers.OpenAI.";
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
    builder.modelName(modelName);
    // Only set temperature if not using the "o" series
    if (!modelName.startsWith("o")) {
      Double temperature = Double.valueOf(GPT_5.equalsIgnoreCase(modelName) ? DEFAULT_TEMPERATURE_GPT_5 : DEFAULT_TEMPERATURE);
      builder.temperature(temperature);
    }

    return builder;
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true);
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
    return builder;
  }

  private static String resolveModelName(String modelName) {
    String selected = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(OpenAiConf.DEFAULT_MODEL), DEFAULT_MODEL));

    validateModel(selected);
    return selected;
  }

  private static void validateModel(String modelName) {
    boolean isKnown = Arrays.stream(OpenAiChatModelName.values()).map(Enum::toString)
        .anyMatch(name -> name.equals(modelName));

    if (!isKnown) {
      Ivy.log().warn("The compatibility of model '" + modelName + "' is unknown.");
    }
  }
}
