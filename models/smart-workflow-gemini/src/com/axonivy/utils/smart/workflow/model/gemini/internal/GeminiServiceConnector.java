package com.axonivy.utils.smart.workflow.model.gemini.internal;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder;

public class GeminiServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;
  private static final String DEFAULT_MODEL = GoogleAiGeminiChatModelName.GEMINI_2_5_FLASH.toString();

  public interface GeminiConf {
    String PREFIX = "AI.Providers.Gemini.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static GoogleAiGeminiChatModelBuilder buildGeminiModel(String modelName) {
    return initBuilder(resolveModelName(modelName));
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

  private static String resolveModelName(String modelName) {
    String selected = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(GeminiConf.DEFAULT_MODEL), DEFAULT_MODEL));

    validateModel(selected);
    return selected;
  }

  private static void validateModel(String modelName) {
    boolean isKnown = Arrays.stream(GoogleAiGeminiChatModelName.values()).map(Enum::toString)
        .anyMatch(name -> name.equals(modelName));

    if (!isKnown) {
      Ivy.log().warn("Unknown Gemini model: '" + modelName + "'. Compatibility not guaranteed.");
    }
  }

}