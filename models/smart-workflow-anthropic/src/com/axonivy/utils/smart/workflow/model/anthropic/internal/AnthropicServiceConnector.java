package com.axonivy.utils.smart.workflow.model.anthropic.internal;

import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveMaxOutputTokens;
import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveTemperature;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel.AnthropicChatModelBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

public class AnthropicServiceConnector {

  private static final String DEFAULT_MODEL = AnthropicChatModelName.CLAUDE_HAIKU_4_5_20251001.toString();
  private static final Set<String> SUPPORTED_EFFORTS = Set.of("low", "medium", "high", "xhigh", "max");

  private static final Set<String> KNOWN_ALIASES = Set.of(
      "claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5", "claude-opus-4-1",
      "claude-sonnet-4-0", "claude-opus-4-0");

  public interface AnthropicConf {
    String PREFIX = "AI.Providers.Anthropic.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static AnthropicChatModelBuilder buildAnthropicModel(ModelOptions options) {
    var effort = options.reasoningEffort();
    validateReasoningEffort(effort);
    AnthropicChatModelBuilder builder = initBuilder(effort);
    var request = ChatRequestParameters.builder()
        .modelName(resolveModelName(options.modelName()))
        .temperature(resolveTemperature(options.temperature()))
        .maxOutputTokens(resolveMaxOutputTokens(options.maxOutputTokens()));

    builder.defaultRequestParameters(request.build());
    return builder;
  }

  private static AnthropicChatModelBuilder initBuilder(String reasoningEffort) {
    var httpClientBuilder = new SmartHttpClientBuilderFactory().create();
    if (StringUtils.isNotBlank(reasoningEffort)) {
      httpClientBuilder = new AnthropicEffortHttpClientBuilder(httpClientBuilder, reasoningEffort);
    }
    AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
        .httpClientBuilder(httpClientBuilder)
        .logRequests(true)
        .logResponses(true);
    var baseUrl = Ivy.var().get(AnthropicConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    String key = Ivy.var().get(AnthropicConf.API_KEY);
    if (!key.isBlank()) {
      builder.apiKey(key);
    }
    return builder;
  }

  private static void validateReasoningEffort(String reasoningEffort) {
    if (StringUtils.isNotBlank(reasoningEffort) && !SUPPORTED_EFFORTS.contains(reasoningEffort)) {
      throw new IllegalArgumentException("Anthropic reasoning effort must be one of " + SUPPORTED_EFFORTS
          + "; the available levels depend on the selected Claude model.");
    }
  }

  private static String resolveModelName(String modelName) {
    String selected = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(AnthropicConf.DEFAULT_MODEL), DEFAULT_MODEL));

    validateModelName(selected);
    return selected;
  }

  private static void validateModelName(String modelName) {
    boolean isKnown = KNOWN_ALIASES.contains(modelName)
        || Stream.of(AnthropicChatModelName.values())
            .map(AnthropicChatModelName::toString)
            .anyMatch(name -> name.equals(modelName));

    if (!isKnown) {
      Ivy.log().warn("The compatibility of model '" + modelName + "' is unknown.");
    }
  }
}
