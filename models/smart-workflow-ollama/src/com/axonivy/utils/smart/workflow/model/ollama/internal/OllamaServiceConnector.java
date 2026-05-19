package com.axonivy.utils.smart.workflow.model.ollama.internal;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder;

public class OllamaServiceConnector {

  private static final double DEFAULT_TEMPERATURE = 0.0;
  private static final String FALLBACK_BASE_URL = "http://localhost:11434";
  private static final String FALLBACK_DEFAULT_MODEL = "llama3.2";
  static final Duration FALLBACK_TIMEOUT = Duration.ofMinutes(5);

  public interface OllamaConf {
    String PREFIX = "AI.Providers.Ollama.";
    String BASE_URL = PREFIX + "BaseUrl";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
    String DEFAULT_EMBEDDING_MODEL = PREFIX + "DefaultEmbeddingModel";
    String TIMEOUT_SECONDS = PREFIX + "TimeoutSeconds";
  }

  public static OllamaChatModelBuilder buildChatModel(String modelName) {
    String selectedModel = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(OllamaConf.DEFAULT_MODEL), FALLBACK_DEFAULT_MODEL));

    return OllamaChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .baseUrl(baseUrl())
        .modelName(selectedModel)
        .temperature(DEFAULT_TEMPERATURE)
        .timeout(timeout())
        .logRequests(true)
        .logResponses(true);
  }

  public static String baseUrl() {
    return StringUtils.defaultIfBlank(Ivy.var().get(OllamaConf.BASE_URL), FALLBACK_BASE_URL);
  }

  public static Duration timeout() {
    String raw = Ivy.var().get(OllamaConf.TIMEOUT_SECONDS);
    if (StringUtils.isBlank(raw)) {
      return FALLBACK_TIMEOUT;
    }
    try {
      long seconds = Long.parseLong(raw.trim());
      if (seconds <= 0) {
        Ivy.log().warn("'" + OllamaConf.TIMEOUT_SECONDS + "' must be positive (got " + seconds
            + "). Falling back to " + FALLBACK_TIMEOUT.toSeconds() + "s.");
        return FALLBACK_TIMEOUT;
      }
      return Duration.ofSeconds(seconds);
    } catch (NumberFormatException e) {
      Ivy.log().warn("Invalid '" + OllamaConf.TIMEOUT_SECONDS + "' value '" + raw
          + "'. Expected a positive integer (seconds). Falling back to " + FALLBACK_TIMEOUT.toSeconds() + "s.");
      return FALLBACK_TIMEOUT;
    }
  }
}
