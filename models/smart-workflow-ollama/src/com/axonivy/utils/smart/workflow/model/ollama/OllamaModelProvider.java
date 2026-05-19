package com.axonivy.utils.smart.workflow.model.ollama;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaServiceConnector;
import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaServiceConnector.OllamaConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

public class OllamaModelProvider implements ChatModelProvider {

  public static final String NAME = "Ollama";

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = OllamaServiceConnector.buildChatModel(options.modelName());
    if (options.structuredOutput()) {
      builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
          .responseFormat(ResponseFormat.JSON);
    }
    builder.listeners(options.listeners());
    return builder.build();
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public List<String> models() {
    return List.of();
  }

  @Override
  public List<String> secretsVars() {
    return List.of();
  }

  @Override
  public boolean supportsEmbedding() {
    return true;
  }

  @Override
  public String resolveEmbeddingModelName(EmbeddingModelOptions options) {
    return StringUtils.defaultIfBlank(options.modelName(),
        StringUtils.defaultIfBlank(Ivy.var().get(OllamaConf.DEFAULT_EMBEDDING_MODEL), "nomic-embed-text"));
  }

  @Override
  public Optional<EmbeddingModel> setupEmbedding(EmbeddingModelOptions options) {
    String modelName = resolveEmbeddingModelName(options);
    var builder = OllamaEmbeddingModel.builder()
        .baseUrl(OllamaServiceConnector.baseUrl())
        .modelName(modelName)
        .timeout(OllamaServiceConnector.timeout())
        .logRequests(true)
        .logResponses(true);
    return Optional.of(builder.build());
  }
}
