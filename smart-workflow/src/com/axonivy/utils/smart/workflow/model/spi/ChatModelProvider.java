package com.axonivy.utils.smart.workflow.model.spi;

import java.util.List;
import java.util.Optional;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface ChatModelProvider {

  String name();
  ChatModel setup(ModelOptions options);
  List<String> models();
  List<String> secretsVars();

  public static record EmbeddingModelOptions(String modelName, String apiKey) {

    public EmbeddingModelOptions() {
      this(null, null);
    }

    public static EmbeddingModelOptions options() {
      return new EmbeddingModelOptions();
    }

    public EmbeddingModelOptions modelName(String name) {
      return new EmbeddingModelOptions(name, apiKey);
    }

    public EmbeddingModelOptions apiKey(String key) {
      return new EmbeddingModelOptions(modelName, key);
    }
  }

  default boolean supportsEmbedding() {
    return false;
  }

  default String resolveEmbeddingModelName(EmbeddingModelOptions options) {
    return Optional.ofNullable(options)
        .map(EmbeddingModelOptions::modelName)
        .orElse("");
  }

  default Optional<EmbeddingModel> setupEmbedding(EmbeddingModelOptions options) {
    return Optional.empty();
  }

}
