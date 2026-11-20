package com.axonivy.utils.smart.workflow.model.gemini.internal;

import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveMaxOutputTokens;
import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveTemperature;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder;

public class GeminiServiceConnector {

  private static final String DEFAULT_MODEL = GoogleAiGeminiChatModelName.GEMINI_2_5_FLASH.toString();
  private static final Set<String> SUPPORTED_EFFORTS = Set.of("none", "minimal", "low", "medium", "high");

  public interface GeminiConf {
    String PREFIX = "AI.Providers.Gemini.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static GoogleAiGeminiChatModelBuilder buildGeminiModel(ModelOptions options) {
    GoogleAiGeminiChatModelBuilder builder = initBuilder();
    String modelName = resolveModelName(options.modelName());
    var request = ChatRequestParameters.builder()
        .modelName(modelName)
        .temperature(resolveTemperature(options.temperature()))
        .maxOutputTokens(resolveMaxOutputTokens(options.maxOutputTokens()));

    configureReasoningEffort(builder, options.reasoningEffort());
    builder.defaultRequestParameters(request.build());
    return builder;
  }

  private static void configureReasoningEffort(GoogleAiGeminiChatModelBuilder builder, String reasoningEffort) {
    if (StringUtils.isBlank(reasoningEffort)) {
      return;
    }
    if (!SUPPORTED_EFFORTS.contains(reasoningEffort)) {
      throw new IllegalArgumentException("Gemini reasoning effort must be one of " + SUPPORTED_EFFORTS
          + "; the available levels depend on the selected Gemini model.");
    }

    var config = GeminiThinkingConfig.builder();
    if ("none".equals(reasoningEffort)) {
      config.thinkingBudget(0);
    } else {
      config.thinkingLevel(reasoningEffort.toUpperCase(Locale.ROOT));
    }
    builder.thinkingConfig(config.build());
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
