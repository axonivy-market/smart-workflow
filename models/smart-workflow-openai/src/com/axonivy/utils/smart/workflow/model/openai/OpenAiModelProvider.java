package com.axonivy.utils.smart.workflow.model.openai;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

public class OpenAiModelProvider implements ChatModelProvider {

  public static final String NAME = "OpenAI";
  private static final String FALLBACK_EMBEDDING_MODEL = "text-embedding-3-small";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(options.modelName());
    if (options.structuredOutput()) {
      builder.responseFormat("json_schema");
    }
    builder.listeners(options.listeners());
    return builder.build();
  }

  @Override
  public List<String> models() {
    return Stream.of(OpenAiChatModelName.values())
        .map(OpenAiChatModelName::name)
        .toList();
  }

  @Override
  public boolean supportsEmbedding() {
    return true;
  }

  @Override
  public Optional<EmbeddingModel> setupEmbedding(EmbeddingModelOptions options) {
    String modelName = StringUtils.defaultIfBlank(options.modelName(),
        StringUtils.defaultIfBlank(Ivy.var().get(OpenAiConf.DEFAULT_EMBEDDING_MODEL), FALLBACK_EMBEDDING_MODEL));
    String apiKey = resolveApiKey(options.apiKey());

    var builder = OpenAiEmbeddingModel.builder()
        .modelName(modelName)
        .logRequests(true)
        .logResponses(true);

    var baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    if (!apiKey.isBlank()) {
      builder.apiKey(apiKey);
    }

    return Optional.of(builder.build());
  }

  private String resolveApiKey(String optionsApiKey) {
    if (StringUtils.isNotBlank(optionsApiKey)) {
      return optionsApiKey;
    }
    return Ivy.var().get(OpenAiConf.API_KEY);
  }

}
