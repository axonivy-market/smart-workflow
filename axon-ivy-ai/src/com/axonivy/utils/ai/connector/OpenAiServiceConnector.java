package com.axonivy.utils.ai.connector;

import java.util.Map;

import com.axonivy.utils.ai.constant.AiConstant;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector extends AbstractAiServiceConnector {
  private static final long serialVersionUID = -7887376408428122870L;

  public interface OpenAiConf {
    String PREFIX = "Ai.OpenAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String TEST_HEADER = PREFIX + "Headers.test";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel() {
    return initBuilder();
  }

  public static OpenAiChatModelBuilder buildJsonOpenAiModel() {
    var builder = initBuilder()
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
    return builder;
  }

  private static OpenAiChatModelBuilder initBuilder() {
    OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
        .logRequests(true)
        .logResponses(true)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .temperature(Double.valueOf(AiConstant.TEMPERATURE));
    var baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    var testing = Ivy.var().get(OpenAiConf.TEST_HEADER);
    if (!testing.isBlank()) {
      builder.customHeaders(Map.of("X-Requested-By", "ivy", "X-Test", testing));
    } else {
      builder.apiKey(Ivy.var().get(OpenAiConf.API_KEY));
    }
    return builder;
  }
}
