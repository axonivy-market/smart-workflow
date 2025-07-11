package com.axonivy.utils.ai.connector;

import java.io.Serializable;
import java.util.Map;

import com.axonivy.utils.ai.exception.ai.OpenAIErrorResponse;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

public abstract class AbstractAiServiceConnector implements Serializable {

  private static final long serialVersionUID = -5201325150657914187L;

  protected String apiKey;

  public abstract void init(String modelName, String apiKey);

  public abstract OpenAIErrorResponse testConnection();

  public abstract String generate(String message);

  public abstract String generate(Map<String, Object> variables, String promptTemplate);

  public abstract String stream(Map<String, Object> variables, String promptTemplate,
      StreamingChatResponseHandler handler);
}
