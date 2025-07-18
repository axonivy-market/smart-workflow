package com.axonivy.utils.ai.connector;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.constant.AiConstant;
import com.axonivy.utils.ai.enums.model.OpenAiModelType;
import com.axonivy.utils.ai.exception.ai.OpenAIErrorResponse;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;

public class OpenAiServiceConnector extends AbstractAiServiceConnector {
  private static final long serialVersionUID = -7887376408428122870L;

  private OpenAiOfficialChatModel chatModel;
  private OpenAiOfficialStreamingChatModel streamModel;

  public static OpenAiServiceConnector getBigBrain() {
    OpenAiServiceConnector result = new OpenAiServiceConnector();
    result.init(OpenAiModelType.GPT_4O.getName(), Ivy.var().get("Ai.OpenAI.APIKey"));
    return result;
  }

  public static OpenAiServiceConnector getTinyBrain() {
    OpenAiServiceConnector result = new OpenAiServiceConnector();
    result.init(OpenAiModelType.GPT_4O_MINI.getName(), Ivy.var().get("Ai.OpenAI.APIKey"));
    return result;
  }

  @Override
  public void init(String modelName, String apiKey) {
    this.apiKey = apiKey;
    this.chatModel = OpenAiOfficialChatModel.builder().apiKey(apiKey)
        .modelName(OpenAiModelType.findType(modelName).getName()).temperature(Double.valueOf(AiConstant.TEMPERATURE))
        .build();

    this.streamModel = OpenAiOfficialStreamingChatModel.builder().apiKey(apiKey)
        .modelName(OpenAiModelType.findType(modelName).getName()).temperature(Double.valueOf(AiConstant.TEMPERATURE))
        .build();
  }

  @Override
  public OpenAIErrorResponse testConnection() {
    try {
      chatModel.chat(Arrays.asList(new UserMessage("hello")));
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error;
    }
    return null;
  }

  @Override
  public String generate(String message) {
    try {
      // Ivy.log().error("Input");
      // Ivy.log().error(message);
      String result = chatModel.chat(message);
      // Ivy.log().error("result");
      // Ivy.log().error(result);
      return result;
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

  @Override
  public String stream(Map<String, Object> variables, String promptTemplate, StreamingChatResponseHandler handler) {
    try {
      streamModel.chat(PromptTemplate.from(promptTemplate).apply(variables).text(), handler);
    } catch (Exception e) {
      OpenAIErrorResponse error = BusinessEntityConverter.jsonValueToEntity(e.getCause().getMessage(),
          OpenAIErrorResponse.class);
      return error.getError().getMessage();
    }
    return StringUtils.EMPTY;
  }
}
