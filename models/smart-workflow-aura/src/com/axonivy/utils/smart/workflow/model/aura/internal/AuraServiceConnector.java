package com.axonivy.utils.smart.workflow.model.aura.internal;

import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveMaxOutputTokens;
import static com.axonivy.utils.smart.workflow.model.utils.ModelOptionUtils.resolveTemperature;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.client.SmartHttpClientBuilderFactory;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

public class AuraServiceConnector {

  public interface AuraConf {
    String PREFIX = "AI.Providers.Aura.";
    String BASE_URL = PREFIX + "BaseUrl";
    String API_KEY = PREFIX + "APIKey";
    String DEFAULT_MODEL = PREFIX + "DefaultModel";
  }

  public static OpenAiChatModelBuilder buildOpenAiModel(ModelOptions options) {
    var baseUrl = Ivy.var().get(AuraConf.BASE_URL);
    if (StringUtils.isBlank(baseUrl)) {
      throw new IllegalArgumentException("Configure " + AuraConf.BASE_URL + " before calling Aura");
    }
    var modelName = StringUtils.defaultIfBlank(options.modelName(), Ivy.var().get(AuraConf.DEFAULT_MODEL));
    if (StringUtils.isBlank(modelName)) {
      throw new IllegalArgumentException("Configure " + AuraConf.DEFAULT_MODEL + " or select a model on the agent");
    }
    var request = ChatRequestParameters.builder()
        .modelName(modelName)
        .temperature(resolveTemperature(options.temperature()))
        .maxOutputTokens(resolveMaxOutputTokens(options.maxOutputTokens()))
        .build();
    var builder = OpenAiChatModel.builder()
        .httpClientBuilder(new SmartHttpClientBuilderFactory().create())
        .baseUrl(baseUrl)
        .defaultRequestParameters(request)
        .logRequests(true)
        .logResponses(true);
    var key = Ivy.var().get(AuraConf.API_KEY);
    if (StringUtils.isNotBlank(key)) {
      builder.apiKey(key);
    } else {
      builder.customHeaders(Map.of("X-Requested-By", "ivy")); // allow local mock calls
    }
    return builder;
  }
}
