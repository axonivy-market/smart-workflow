package com.axonivy.utils.smart.workflow.model.openai.internal;

import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.DEFAULT_TEMPERATURE;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatRequestParameters;

public class OpenAiServiceConnector {

  // Temporary model name and temperature for GPT-5. Will remove once LangChain4j
  // fully support GPT-5
  private static final String GPT_5 = "gpt-5";
  private static final int DEFAULT_TEMPERATURE_GPT_5 = 1;
  private static final Pattern REASONING_EFFORT_MODELS = Pattern.compile("^(?:o[134](?:[-.]|$)|gpt-[56](?:[-.]|$))",
      Pattern.CASE_INSENSITIVE);

  private static final String DEFAULT_MODEL = OpenAiChatModelName.GPT_4_1_MINI.toString();

  public interface OpenAiConf {
    String PREFIX = "AI.Providers.OpenAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
    String DEFAULT_EMBEDDING_MODEL = PREFIX + "DefaultEmbeddingModel";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return initBuilder(resolveModelName(DEFAULT_MODEL), null, null);
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return initBuilder(resolveModelName(DEFAULT_MODEL), null, null)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  public static OpenAiChatModelBuilder buildOpenAiModel(ModelOptions options) {
    return initBuilder(resolveModelName(options.modelName()), options.temperature(), options.maxOutputTokens());
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel(ModelOptions options) {
    return initBuilder(resolveModelName(options.modelName()), options.temperature(), options.maxOutputTokens())
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  public static OpenAiResponsesChatModel.Builder buildOpenAiResponsesModel(ModelOptions options) {
    return initResponsesBuilder(resolveModelName(options.modelName()), options.temperature(),
        options.maxOutputTokens(), options.reasoningEffort());
  }

  public static boolean supportsReasoningEffort(String modelName) {
    return REASONING_EFFORT_MODELS.matcher(configuredModelName(modelName)).find();
  }

  private static OpenAiChatModelBuilder initBuilder(String modelName, Double temperature, Integer maxOutputTokens) {
    OpenAiChatModelBuilder model = initBuilder();

    var request = OpenAiChatRequestParameters.builder()
        .modelName(modelName);
    temperature(modelName, temperature)
        .ifPresent(request::temperature);
    Optional.ofNullable(maxOutputTokens)
        .ifPresent(request::maxOutputTokens);

    model.defaultRequestParameters(request.build());

    return model;
  }

  private static OpenAiResponsesChatModel.Builder initResponsesBuilder(String modelName, Double temperature,
      Integer maxOutputTokens, String reasoningEffort) {
    OpenAiResponsesChatModel.Builder model = initResponsesBuilder();
    var request = OpenAiResponsesChatRequestParameters.builder().modelName(modelName);
    temperature(modelName, temperature)
        .ifPresent(request::temperature);
    Optional.ofNullable(maxOutputTokens)
        .ifPresent(request::maxOutputTokens);
    if (StringUtils.isNotBlank(reasoningEffort) && supportsReasoningEffort(modelName)) {
      Ivy.log().error(
          "Using reasoning effort for model '" + modelName + "' with reasoning effort '" + reasoningEffort + "'");
      request.reasoningEffort(reasoningEffort);
    } else if (StringUtils.isNotBlank(reasoningEffort)) {
      Ivy.log().warn("Ignoring OpenAI reasoning effort for model '" + modelName
          + "' because the model does not support it.");
    }

    model.defaultRequestParameters(request.build());
    return model;
  }

  private static Optional<Double> temperature(String modelName, Double temperature) {
    if (modelName.startsWith("o")) {
      // Only set temperature if not using the "o" series
      return Optional.empty();
    }
    if (Strings.CI.startsWith(modelName, GPT_5)) {
      return Optional.of(Double.valueOf(DEFAULT_TEMPERATURE_GPT_5));
    }

    return Optional.of(temperature == null ? DEFAULT_TEMPERATURE : temperature);
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true);
    configureEndpoint(builder::baseUrl, builder::apiKey, builder::customHeaders);
    return builder;
  }

  private static OpenAiResponsesChatModel.Builder initResponsesBuilder() {
    OpenAiResponsesChatModel.Builder builder = OpenAiResponsesChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .logRequests(true)
        .logResponses(true);
    configureEndpoint(builder::baseUrl, builder::apiKey, builder::customHeaders);
    return builder;
  }

  private static void configureEndpoint(java.util.function.Consumer<String> baseUrlSetter,
      java.util.function.Consumer<String> apiKeySetter,
      java.util.function.Consumer<Map<String, String>> headersSetter) {
    var baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      baseUrlSetter.accept(baseUrl);
    }
    String key = Ivy.var().get(OpenAiConf.API_KEY);
    if (!key.isBlank()) {
      apiKeySetter.accept(key);
    } else {
      headersSetter.accept(Map.of("X-Requested-By", "ivy")); // TODO as pure test variable
    }
  }

  private static String resolveModelName(String modelName) {
    String selected = configuredModelName(modelName);
    validateModel(selected);
    return selected;
  }

  private static String configuredModelName(String modelName) {
    return StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(OpenAiConf.DEFAULT_MODEL), DEFAULT_MODEL));
  }

  private static void validateModel(String modelName) {
    boolean isKnown = Arrays.stream(OpenAiChatModelName.values()).map(Enum::toString)
        .anyMatch(name -> name.equals(modelName));

    if (!isKnown) {
      String baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
      if (StringUtils.isNotBlank(baseUrl)) {
        Ivy.log().info("Model '" + modelName
            + "' is not in the OpenAI model list. Continue with custom endpoint '" + baseUrl + "'.");
      } else {
        Ivy.log().warn("The compatibility of model '" + modelName + "' is unknown.");
      }
    }
  }
}
