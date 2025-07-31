package com.axonivy.utils.ai.connector;

import java.util.Arrays;
import java.util.Map;

import com.axonivy.utils.ai.constant.AiConstant;
import com.axonivy.utils.ai.enums.model.OpenAiModelType;
import com.axonivy.utils.ai.exception.ai.OpenAIErrorResponse;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAiServiceConnector extends AbstractAiServiceConnector {
  private static final long serialVersionUID = -7887376408428122870L;

  private OpenAiChatModel chatModel;
  private OpenAiChatModel jsonModel;

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
    this.jsonModel = buildJsonOpenAiModel()
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
    var testing = Ivy.var().get(OpenAiConf.TEST_HEADER);
    if (!testing.isBlank()) {
      builder.customHeaders(Map.of("X-Requested-By", "ivy", "X-Test", testing));
    } else {
      builder.apiKey(Ivy.var().get(OpenAiConf.API_KEY));
    }
    return builder;
  }
  
  public OpenAiChatModelBuilder buildJsonOpenAiModel() {
    return OpenAiChatModel.builder().apiKey(Ivy.var().get("Ai.OpenAI.APIKey"))
        .logRequests(true)
        .logResponses(true)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .temperature(Double.valueOf(AiConstant.TEMPERATURE))
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true);
  }

  @Override
  public OpenAIErrorResponse testConnection() {
    try {
      getChatModel().chat(Arrays.asList(new UserMessage("hello")));
    } catch (Exception e) {
      return BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
    }
    return null;
  }

  @Override
  public String generate(String message) {
    try {
      return getChatModel().chat(message);
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
  }

  @Override
  public String generate(Map<String, Object> variables, String promptTemplate) {
    try {
      return getChatModel().chat(PromptTemplate.from(promptTemplate).apply(variables).text());
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
  }

  @Override
  public String generateJson(JsonSchema jsonSchema, Map<String, Object> variables, String promptTemplate) {
    try {

      Ivy.log().error("input");
      Ivy.log().error(PromptTemplate.from(promptTemplate).apply(variables).text());
      ResponseFormat responseFormat = ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(jsonSchema)
          .build();
      
      ChatRequest chatRequest = ChatRequest.builder().responseFormat(responseFormat)
          .messages(PromptTemplate.from(promptTemplate).apply(variables).toUserMessage()).build();

      ChatResponse result = getJsonModel().chat(chatRequest);

      Ivy.log().error("output");
      Ivy.log().error(result.aiMessage().text());

      // Need to implement guardrail
      return result.aiMessage().text();
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
  }

  public OpenAiChatModel getChatModel() {
    return chatModel;
  }

  public void setChatModel(OpenAiChatModel chatModel) {
    this.chatModel = chatModel;
  }

  public OpenAiChatModel getJsonModel() {
    return jsonModel;
  }

  public void setJsonModel(OpenAiChatModel jsonModel) {
    this.jsonModel = jsonModel;
  }

}
