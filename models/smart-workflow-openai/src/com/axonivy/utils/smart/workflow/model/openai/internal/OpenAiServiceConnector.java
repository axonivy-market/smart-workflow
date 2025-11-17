package com.axonivy.utils.smart.workflow.model.openai.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.google.common.base.Objects;

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

  private static String validateModelName(String modelName) {
    String selected = Optional.ofNullable(modelName)
        .filter(Predicate.not(String::isBlank))
        .or(() -> Optional.of(Ivy.var().get(OpenAiConf.MODEL)))
        .filter(Predicate.not(String::isBlank))
        .orElse(DEFAULT_MODEL);

    var known = Arrays.stream(OpenAiChatModelName.values())
        .map(OpenAiChatModelName::toString)
        .filter(name -> Objects.equal(name, selected))
        .findAny();
    if (known.isEmpty()) {
      Ivy.log().warn("The compatibility of model '" + selected + "' is unknown.");
    }

    return selected;
  }
}
