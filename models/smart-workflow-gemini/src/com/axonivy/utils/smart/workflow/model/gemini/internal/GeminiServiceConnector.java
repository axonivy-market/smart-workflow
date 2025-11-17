package com.axonivy.utils.smart.workflow.model.gemini.internal;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.google.common.base.Objects;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder;

public class GeminiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;
  private static final String DEFAULT_MODEL = GoogleAiGeminiChatModelName.GEMINI_1_5_FLASH.toString();

  public interface GeminiConf {
    String PREFIX = "AI.Providers.Gemini.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String MODEL = PREFIX + "Model";
  }

  public static GoogleAiGeminiChatModelBuilder buildGeminiModel() {
    return buildGeminiModel(DEFAULT_MODEL);
  }

  public static GoogleAiGeminiChatModelBuilder buildJsonGeminiModel() {
    return buildJsonGeminiModel(DEFAULT_MODEL);
  }

  public static GoogleAiGeminiChatModelBuilder buildGeminiModel(String modelName) {
    return initBuilder(validateModelName(modelName));
  }

  public static GoogleAiGeminiChatModelBuilder buildJsonGeminiModel(String modelName) {
    return initBuilder(validateModelName(modelName)).supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  private static GoogleAiGeminiChatModelBuilder initBuilder(String modelName) {
    GoogleAiGeminiChatModelBuilder builder = initBuilder();
    builder.modelName(modelName);
    builder.temperature(Double.valueOf(DEFAULT_TEMPERATURE));
    return builder;
  }

  private static GoogleAiGeminiChatModelBuilder initBuilder() {
    GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create()).logRequestsAndResponses(true);

    var baseUrl = Ivy.var().get(GeminiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }

    String key = Ivy.var().get(GeminiConf.API_KEY);
    if (!key.isBlank()) {
      builder.apiKey(key);
    } else {
      builder.apiKey("test-api-key"); // TODO as pure test variable
    }
    return builder;
  }

  private static String validateModelName(String modelName) {
    String selected = Optional.ofNullable(modelName).filter(Predicate.not(String::isBlank))
        .or(() -> Optional.of(Ivy.var().get(GeminiConf.MODEL))).filter(Predicate.not(String::isBlank))
        .orElse(DEFAULT_MODEL);

    var known = Arrays.stream(GoogleAiGeminiChatModelName.values()).map(GoogleAiGeminiChatModelName::toString)
        .filter(name -> Objects.equal(name, selected)).findAny();
    if (known.isEmpty()) {
      Ivy.log().warn("The compatibility of model '" + selected + "' is unknown.");
    }

    return selected;
  }
}