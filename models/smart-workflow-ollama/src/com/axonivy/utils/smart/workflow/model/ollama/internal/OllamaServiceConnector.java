package com.axonivy.utils.smart.workflow.model.ollama.internal;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder;

public class OllamaServiceConnector {

  private static final int DEFAULT_TEMPERATURE = 0;
  private static final String FALLBACK_BASE_URL = "http://localhost:11434";
  private static final String FALLBACK_DEFAULT_MODEL = "llama3.2";

  public interface OllamaConf {
    String PREFIX = "AI.Providers.Ollama.";
    String BASE_URL = PREFIX + "BaseUrl";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
    String DEFAULT_EMBEDDING_MODEL = PREFIX + "DefaultEmbeddingModel";
  }

  public static OllamaChatModelBuilder buildChatModel(String modelName) {
    String selectedModel = StringUtils.defaultIfBlank(modelName,
        StringUtils.defaultIfBlank(Ivy.var().get(OllamaConf.DEFAULT_MODEL), FALLBACK_DEFAULT_MODEL));

    return OllamaChatModel.builder()
        .baseUrl(baseUrl())
        .modelName(selectedModel)
        .temperature(Double.valueOf(DEFAULT_TEMPERATURE))
        .logRequests(true)
        .logResponses(true);
  }

  public static String baseUrl() {
    return StringUtils.defaultIfBlank(Ivy.var().get(OllamaConf.BASE_URL), FALLBACK_BASE_URL);
  }
}
