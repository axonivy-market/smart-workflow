package com.axonivy.utils.smart.workflow.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.EmbeddingModelOptions;
import com.axonivy.utils.smart.workflow.rag.RagConf;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class EmbeddingModelFactory {

  public record EmbeddingConfig(String providerName, String modelName) {}

  public static EmbeddingConfig resolvedEmbeddingConfig() {
    String providerVar = Ivy.var().get(RagConf.EMBEDDING_PROVIDER);
    String modelVar = Ivy.var().get(RagConf.EMBEDDING_MODEL_NAME);
    String apiKey = Ivy.var().get(RagConf.EMBEDDING_API_KEY);
    ChatModelProvider provider = getProviderOrDefault(providerVar);
    EmbeddingModelOptions opts = EmbeddingModelOptions.options().modelName(modelVar).apiKey(apiKey);
    return new EmbeddingConfig(provider.name(), provider.resolveEmbeddingModelName(opts));
  }

  public static EmbeddingModel createModel(EmbeddingModelOptions options, String providerName) {
    return getProviderOrDefault(providerName).setupEmbedding(options).orElseThrow();
  }

  public static EmbeddingModel createFromIvyVars() {
    String provider = Ivy.var().get(RagConf.EMBEDDING_PROVIDER);
    String modelName = Ivy.var().get(RagConf.EMBEDDING_MODEL_NAME);
    String apiKey = Ivy.var().get(RagConf.EMBEDDING_API_KEY);
    EmbeddingModelOptions options = EmbeddingModelOptions.options()
        .modelName(StringUtils.defaultIfBlank(modelName, null))
        .apiKey(StringUtils.defaultIfBlank(apiKey, null));
    return getProviderOrDefault(provider).setupEmbedding(options).orElseThrow();
  }

  public static ChatModelProvider getProviderOrDefault(String providerName) {
    String resolved = StringUtils.defaultIfBlank(providerName,
        StringUtils.defaultIfBlank(Ivy.var().get(ChatModelFactory.AiConf.DEFAULT_PROVIDER), "OpenAI"));
    return ChatModelFactory.create(resolved)
        .filter(ChatModelProvider::supportsEmbedding)
        .orElseThrow(() -> new IllegalArgumentException(
            "Provider " + resolved + " does not support embedding or is not installed."));
  }

  public static Optional<ChatModelProvider> create(String provider) {
    return providers().stream()
        .filter(impl -> Objects.equals(impl.name(), provider))
        .findFirst();
  }

  public static Set<ChatModelProvider> providers() {
    return ChatModelFactory.providers().stream()
        .filter(ChatModelProvider::supportsEmbedding)
        .collect(Collectors.toSet());
  }

}
