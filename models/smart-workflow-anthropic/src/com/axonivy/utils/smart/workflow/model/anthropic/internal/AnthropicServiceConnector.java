package com.axonivy.utils.smart.workflow.model.anthropic.internal;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.anthropic.AnthropicChatModel.AnthropicChatModelBuilder;

public class AnthropicServiceConnector {

  private static final String DEFAULT_MODEL = AnthropicChatModelName.CLAUDE_HAIKU_4_5_20251001.toString();

  private static final Set<String> KNOWN_ALIASES = Set.of(
    "claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5", "claude-opus-4-1",
    "claude-sonnet-4-0", "claude-opus-4-0"
  );

  public interface AnthropicConf {
    String PREFIX = "AI.Providers.Anthropic.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static AnthropicChatModelBuilder buildAnthropicModel() {
    return buildAnthropicModel(DEFAULT_MODEL);
  }

  public static AnthropicChatModelBuilder buildAnthropicModel(String modelName) {
    return initBuilder(resolveModelName(modelName));
  }

  private static AnthropicChatModelBuilder initBuilder(String modelName) {
    AnthropicChatModelBuilder builder = initBuilder();
    var request = ChatRequestParameters.builder()
      .modelName(modelName);
    builder.defaultRequestParameters(request.build()); 
    return builder;
  }

  private static AnthropicChatModelBuilder initBuilder() {
    AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
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
