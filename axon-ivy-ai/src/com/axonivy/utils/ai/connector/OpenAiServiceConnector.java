package com.axonivy.utils.ai.connector;

import java.util.Arrays;
import java.util.Map;

import com.axonivy.utils.ai.constant.AiConstant;
import com.axonivy.utils.ai.enums.model.OpenAiModelType;
import com.axonivy.utils.ai.exception.ai.OpenAIErrorResponse;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector extends AbstractAiServiceConnector {
  private static final long serialVersionUID = -7887376408428122870L;

  private OpenAiChatModel chatModel;

  public static OpenAiServiceConnector getBigBrain() {
    OpenAiServiceConnector result = new OpenAiServiceConnector();
    result.init(OpenAiModelType.GPT_4O.getName());
    return result;
  }

  public static OpenAiServiceConnector getTinyBrain() {
    OpenAiServiceConnector result = new OpenAiServiceConnector();
    result.init(OpenAiModelType.GPT_4O_MINI.getName());
    return result;
  }

  @Override
  public void init(String modelName) {
    this.chatModel = buildOpenAiModel()
        .modelName(modelName)
        .build();
  }

  public interface OpenAiConf {
    String PREFIX = "Ai.OpenAI.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String TEST_HEADER = PREFIX + "Headers.test";
  }

  public OpenAiChatModelBuilder buildOpenAiModel() {
    var builder = OpenAiChatModel.builder()
        .logRequests(true)
        .logResponses(true)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .temperature(Double.valueOf(AiConstant.TEMPERATURE));
    var baseUrl = Ivy.var().get(OpenAiConf.BASE_URL);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    String key = Ivy.var().get(OpenAiConf.API_KEY);
    if (!key.isBlank()) {
      builder.apiKey(key);
    } else {
      builder.customHeaders(Map.of("X-Requested-By", "ivy")); // TODO as pure test variable
    }
    return builder;
  }

  @Override
  public OpenAIErrorResponse testConnection() {
    try {
      chatModel.chat(Arrays.asList(new UserMessage("hello")));
    } catch (Exception e) {
      return BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
    }
    return null;
  }

  @Override
  public String generate(String message) {
    try {

      // Ivy.log().error("result");
      // Ivy.log().error(result);
      return chatModel.chat(message);
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
  }

  @Override
  public String generate(Map<String, Object> variables, String promptTemplate) {
    try {
      return chatModel.chat(PromptTemplate.from(promptTemplate).apply(variables).text());
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
  }

}
